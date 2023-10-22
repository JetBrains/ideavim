/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.ComplicatedKeysAction;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.HelperKt;
import com.maddyhome.idea.vim.key.*;
import com.maddyhome.idea.vim.newapi.IjNativeAction;
import com.maddyhome.idea.vim.newapi.IjVimActionsInitiator;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author vlan
 */
@State(name = "VimKeySettings", storages = {@Storage(value = "$APP_CONFIG$/vim_settings.xml")})
public class KeyGroup extends VimKeyGroupBase implements PersistentStateComponent<Element> {
  public static final @NonNls
  String SHORTCUT_CONFLICTS_ELEMENT = "shortcut-conflicts";
  private static final @NonNls
  String SHORTCUT_CONFLICT_ELEMENT = "shortcut-conflict";
  private static final @NonNls
  String OWNER_ATTRIBUTE = "owner";
  private static final String TEXT_ELEMENT = "text";

  private static final Logger logger = Logger.getInstance(KeyGroup.class);

  public void registerRequiredShortcutKeys(@NotNull VimEditor editor) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(getRequiredShortcutKeys()),
        ((IjVimEditor) editor).getEditor().getComponent());
  }

  public void registerShortcutsForLookup(@NotNull LookupImpl lookup) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(getRequiredShortcutKeys()),
        lookup.getComponent(), lookup);
  }

  void unregisterShortcutKeys(@NotNull VimEditor editor) {
    EventFacade.getInstance().unregisterCustomShortcutSet(VimShortcutKeyAction.getInstance(),
      ((IjVimEditor) editor).getEditor().getComponent());
  }

  public boolean showKeyMappings(@NotNull Set<? extends MappingMode> modes, @NotNull Editor editor) {
    List<Pair<EnumSet<MappingMode>, MappingInfo>> rows = getKeyMappingRows(modes);
    final StringBuilder builder = new StringBuilder();
    for (Pair<EnumSet<MappingMode>, MappingInfo> row : rows) {
      MappingInfo mappingInfo = row.getSecond();
      builder.append(StringsKt.padEnd(getModesStringCode(row.getFirst()), 2, ' '));
      builder.append(" ");
      builder.append(StringsKt.padEnd(VimInjectorKt.getInjector().getParser().toKeyNotation(mappingInfo.getFromKeys()), 11, ' '));
      builder.append(" ");
      builder.append(mappingInfo.isRecursive() ? " " : "*");
      builder.append(" ");
      builder.append(mappingInfo.getPresentableString());
      builder.append("\n");
    }
    ExOutputModel.getInstance(editor).output(builder.toString());
    return true;
  }


  @Override
  public void updateShortcutKeysRegistration() {
    for (Editor editor : HelperKt.localEditors()) {
      unregisterShortcutKeys(new IjVimEditor(editor));
      registerRequiredShortcutKeys(new IjVimEditor(editor));
    }
  }

  public void saveData(@NotNull Element element) {
    final Element conflictsElement = new Element(SHORTCUT_CONFLICTS_ELEMENT);
    for (Map.Entry<KeyStroke, ShortcutOwnerInfo> entry : myShortcutConflicts.entrySet()) {
      final ShortcutOwner owner;
      ShortcutOwnerInfo myValue = entry.getValue();
      if (myValue instanceof ShortcutOwnerInfo.AllModes) {
        owner = ((ShortcutOwnerInfo.AllModes) myValue).getOwner();
      } else if (myValue instanceof ShortcutOwnerInfo.PerMode) {
        owner = null;
      } else {
        throw new RuntimeException();
      }
      if (owner != null && owner != ShortcutOwner.UNDEFINED) {
        final Element conflictElement = new Element(SHORTCUT_CONFLICT_ELEMENT);
        conflictElement.setAttribute(OWNER_ATTRIBUTE, owner.getOwnerName());
        final Element textElement = new Element(TEXT_ELEMENT);
        VimPlugin.getXML().setSafeXmlText(textElement, entry.getKey().toString());
        conflictElement.addContent(textElement);
        conflictsElement.addContent(conflictElement);
      }
    }
    element.addContent(conflictsElement);
  }

  public void readData(@NotNull Element element) {
    final Element conflictsElement = element.getChild(SHORTCUT_CONFLICTS_ELEMENT);
    if (conflictsElement != null) {
      final java.util.List<Element> conflictElements = conflictsElement.getChildren(SHORTCUT_CONFLICT_ELEMENT);
      for (Element conflictElement : conflictElements) {
        final String ownerValue = conflictElement.getAttributeValue(OWNER_ATTRIBUTE);
        ShortcutOwner owner = ShortcutOwner.UNDEFINED;
        try {
          owner = ShortcutOwner.fromString(ownerValue);
        } catch (IllegalArgumentException ignored) {
        }
        final Element textElement = conflictElement.getChild(TEXT_ELEMENT);
        if (textElement != null) {
          final String text = VimPlugin.getXML().getSafeXmlText(textElement);
          if (text != null) {
            final KeyStroke keyStroke = KeyStroke.getKeyStroke(text);
            if (keyStroke != null) {
              myShortcutConflicts.put(keyStroke, new ShortcutOwnerInfo.AllModes(owner));
            }
          }
        }
      }
    }
  }

  @Override
  public @NotNull
  List<NativeAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    final Keymap keymap = keymapManager.getActiveKeymap();
    final KeyboardShortcut shortcut = new KeyboardShortcut(keyStroke, null);
    final Map<String, ? extends List<KeyboardShortcut>> conflicts = keymap.getConflicts("", shortcut);
    final List<AnAction> actions = new ArrayList<>();
    for (String actionId : conflicts.keySet()) {
      final AnAction action = ActionManagerEx.getInstanceEx().getAction(actionId);
      if (action != null) {
        actions.add(action);
      }
    }
    return actions.stream().map(IjNativeAction::new).collect(toList());
  }

  public @NotNull
  Map<KeyStroke, ShortcutOwnerInfo> getShortcutConflicts() {
    final Set<RequiredShortcut> requiredShortcutKeys = this.getRequiredShortcutKeys();
    final Map<KeyStroke, ShortcutOwnerInfo> savedConflicts = getSavedShortcutConflicts();
    final Map<KeyStroke, ShortcutOwnerInfo> results = new HashMap<>();
    for (RequiredShortcut requiredShortcut : requiredShortcutKeys) {
      KeyStroke keyStroke = requiredShortcut.getKeyStroke();
      if (!VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
        final List<NativeAction> conflicts = getKeymapConflicts(keyStroke);
        if (!conflicts.isEmpty()) {
          ShortcutOwnerInfo owner = savedConflicts.get(keyStroke);
          if (owner == null) {
            owner = ShortcutOwnerInfo.allUndefined;
          }
          results.put(keyStroke, owner);
        }
      }
    }
    return results;
  }

  /**
   * Registers a shortcut that is handled by KeyHandler#handleKey directly, rather than by an action
   *
   * <p>
   * Digraphs are handled directly by KeyHandler#handleKey instead of via an action, but we need to still make sure the
   * shortcuts are registered, or the key handler won't see them
   * </p>
   *
   * @param keyStroke The shortcut to register
   */
  public void registerShortcutWithoutAction(KeyStroke keyStroke, MappingOwner owner) {
    registerRequiredShortcut(Collections.singletonList(keyStroke), owner);
  }

  public void registerCommandAction(@NotNull VimActionsInitiator actionHolder) {
    IjVimActionsInitiator holder = (IjVimActionsInitiator) actionHolder;

    if (!VimPlugin.getPluginId().equals(holder.getBean().getPluginDescriptor().getPluginId())) {
      logger.error("IdeaVim doesn't accept contributions to `vimActions` extension points. " +
        "Please create a plugin using `VimExtension`. " +
        "Plugin to blame: " +
        holder.getBean().getPluginDescriptor().getPluginId());
      return;
    }

    Set<List<KeyStroke>> actionKeys = holder.getBean().getParsedKeys();
    if (actionKeys == null) {
      final EditorActionHandlerBase action = actionHolder.getInstance();
      if (action instanceof ComplicatedKeysAction) {
        actionKeys = ((ComplicatedKeysAction) action).getKeyStrokesSet();
      } else {
        throw new RuntimeException("Cannot register action: " + action.getClass().getName());
      }
    }

    Set<MappingMode> actionModes = holder.getBean().getParsedModes();
    if (actionModes == null) {
      throw new RuntimeException("Cannot register action: " + holder.getBean().getImplementation());
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      initIdentityChecker();
      for (List<KeyStroke> keys : actionKeys) {
        checkCommand(actionModes, actionHolder.getInstance(), keys);
      }
    }

    for (List<KeyStroke> keyStrokes : actionKeys) {
      registerRequiredShortcut(keyStrokes, MappingOwner.IdeaVim.System.INSTANCE);

      for (MappingMode mappingMode : actionModes) {
        Node<VimActionsInitiator> node = getKeyRoot(mappingMode);
        NodesKt.addLeafs(node, keyStrokes, actionHolder);
      }
    }
  }

  private void registerRequiredShortcut(@NotNull List<KeyStroke> keys, MappingOwner owner) {
    for (KeyStroke key : keys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        getRequiredShortcutKeys().add(new RequiredShortcut(key, owner));
      }
    }
  }

  public static @NotNull
  ShortcutSet toShortcutSet(@NotNull Collection<RequiredShortcut> requiredShortcuts) {
    final List<Shortcut> shortcuts = new ArrayList<>();
    for (RequiredShortcut key : requiredShortcuts) {
      shortcuts.add(new KeyboardShortcut(key.getKeyStroke(), null));
    }
    return new CustomShortcutSet(shortcuts.toArray(new Shortcut[0]));
  }

  private static @NotNull
  List<Pair<EnumSet<MappingMode>, MappingInfo>> getKeyMappingRows(@NotNull Set<? extends MappingMode> modes) {
    final Map<ImmutableList<KeyStroke>, EnumSet<MappingMode>> actualModes = new HashMap<>();
    for (MappingMode mode : modes) {
      final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
      for (List<? extends KeyStroke> fromKeys : mapping) {
        final ImmutableList<KeyStroke> key = ImmutableList.copyOf(fromKeys);
        final EnumSet<MappingMode> value = actualModes.get(key);
        final EnumSet<MappingMode> newValue;
        if (value != null) {
          newValue = value.clone();
          newValue.add(mode);
        } else {
          newValue = EnumSet.of(mode);
        }
        actualModes.put(key, newValue);
      }
    }
    final List<Pair<EnumSet<MappingMode>, MappingInfo>> rows = new ArrayList<>();
    for (Map.Entry<ImmutableList<KeyStroke>, EnumSet<MappingMode>> entry : actualModes.entrySet()) {
      final ArrayList<KeyStroke> fromKeys = new ArrayList<>(entry.getKey());
      final EnumSet<MappingMode> mappingModes = entry.getValue();
      if (!mappingModes.isEmpty()) {
        final MappingMode mode = mappingModes.iterator().next();
        final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
        final MappingInfo mappingInfo = mapping.get(fromKeys);
        if (mappingInfo != null) {
          rows.add(new Pair<>(mappingModes, mappingInfo));
        }
      }
    }
    rows.sort(Comparator.comparing(Pair<EnumSet<MappingMode>, MappingInfo>::getSecond));
    return rows;
  }

  private static @NotNull
  @NonNls
  String getModesStringCode(@NotNull Set<MappingMode> modes) {
    if (modes.equals(MappingMode.NVO)) {
      return "";
    } else if (modes.contains(MappingMode.INSERT)) {
      return "i";
    } else if (modes.contains(MappingMode.NORMAL)) {
      return "n";
    }
    // TODO: Add more codes
    return "";
  }

  private @NotNull
  List<AnAction> getActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    results.addAll(getLocalActions(component, keyStroke));
    results.addAll(getKeymapActions(keyStroke));
    return results;
  }

  @Override
  public @NotNull
  List<NativeAction> getActions(@NotNull VimEditor editor, @NotNull KeyStroke keyStroke) {
    return getActions(((IjVimEditor) editor).getEditor().getComponent(), keyStroke).stream()
      .map(IjNativeAction::new).collect(toList());
  }

  private static @NotNull
  List<AnAction> getLocalActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final KeyboardShortcut keyStrokeShortcut = new KeyboardShortcut(keyStroke, null);
    for (Component c = component; c != null; c = c.getParent()) {
      if (c instanceof JComponent) {
        final List<AnAction> actions = ActionUtil.getActions((JComponent) c);
        for (AnAction action : actions) {
          if (action instanceof VimShortcutKeyAction) {
            continue;
          }
          final Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
          for (Shortcut shortcut : shortcuts) {
            if (shortcut.isKeyboard() && shortcut.startsWith(keyStrokeShortcut) && !results.contains(action)) {
              results.add(action);
            }
          }
        }
      }
    }
    return results;
  }

  private static @NotNull
  List<AnAction> getKeymapActions(@NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    for (String id : keymap.getActionIds(keyStroke)) {
      final AnAction action = ActionManager.getInstance().getAction(id);
      if (action != null) {
        results.add(action);
      }
    }
    return results;
  }

  @Nullable
  @Override
  public Element getState() {
    @NonNls Element element = new Element("key");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }

  @Override
  public boolean showKeyMappings(@NotNull Set<? extends MappingMode> modes, @NotNull VimEditor editor) {
    return showKeyMappings(modes, ((IjVimEditor) editor).getEditor());
  }
}
