/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.util.containers.MultiMap;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.action.change.LazyVimCommand;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtensionFacade;
import com.maddyhome.idea.vim.key.*;
import com.maddyhome.idea.vim.newapi.IjNativeAction;
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
import java.util.*;
import java.util.List;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static java.util.stream.Collectors.toList;

/**
 * @author vlan
 */
@State(name = "VimKeySettings", storages = {@Storage(value = "$APP_CONFIG$/vim_settings.xml")})
public class KeyGroup extends VimKeyGroupBase implements PersistentStateComponent<Element> {
  public static final @NonNls String SHORTCUT_CONFLICTS_ELEMENT = "shortcut-conflicts";
  private static final @NonNls String SHORTCUT_CONFLICT_ELEMENT = "shortcut-conflict";
  private static final @NonNls String OWNER_ATTRIBUTE = "owner";
  private static final String TEXT_ELEMENT = "text";

  public void registerRequiredShortcutKeys(@NotNull VimEditor editor) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(getRequiredShortcutKeys()),
                                 ((IjVimEditor)editor).getEditor().getContentComponent());
  }

  public void registerShortcutsForLookup(@NotNull LookupImpl lookup) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(getRequiredShortcutKeys()),
                                 lookup.getComponent(), lookup);
  }

  void unregisterShortcutKeys(@NotNull VimEditor editor) {
    EventFacade.getInstance().unregisterCustomShortcutSet(VimShortcutKeyAction.getInstance(),
                                                          ((IjVimEditor)editor).getEditor().getContentComponent());
  }

  @Override
  public void updateShortcutKeysRegistration() {
    for (VimEditor editor : injector.getEditorGroup().getEditors()) {
      unregisterShortcutKeys(editor);
      registerRequiredShortcutKeys(editor);
    }
  }

  public void saveData(@NotNull Element element) {
    final Element conflictsElement = new Element(SHORTCUT_CONFLICTS_ELEMENT);
    for (Map.Entry<KeyStroke, ShortcutOwnerInfo> entry : myShortcutConflicts.entrySet()) {
      final ShortcutOwner owner;
      ShortcutOwnerInfo myValue = entry.getValue();
      if (myValue instanceof ShortcutOwnerInfo.AllModes) {
        owner = ((ShortcutOwnerInfo.AllModes)myValue).getOwner();
      }
      else if (myValue instanceof ShortcutOwnerInfo.PerMode) {
        owner = null;
      }
      else {
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
      final List<Element> conflictElements = conflictsElement.getChildren(SHORTCUT_CONFLICT_ELEMENT);
      for (Element conflictElement : conflictElements) {
        final String ownerValue = conflictElement.getAttributeValue(OWNER_ATTRIBUTE);
        ShortcutOwner owner = ShortcutOwner.UNDEFINED;
        try {
          owner = ShortcutOwner.fromString(ownerValue);
        }
        catch (IllegalArgumentException ignored) {
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
  public @NotNull List<NativeAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
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

  public @NotNull Map<KeyStroke, ShortcutOwnerInfo> getShortcutConflicts() {
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

  public void registerCommandAction(@NotNull LazyVimCommand command) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      initIdentityChecker();
      for (List<KeyStroke> keys : command.getKeys()) {
        checkCommand(command.getModes(), command, keys);
      }
    }

    for (List<KeyStroke> keyStrokes : command.getKeys()) {
      registerRequiredShortcut(keyStrokes, MappingOwner.IdeaVim.System.INSTANCE);

      for (MappingMode mappingMode : command.getModes()) {
        getBuiltinCommandsTrie(mappingMode).add(keyStrokes, command);
      }
    }
  }

  private void registerRequiredShortcut(@NotNull List<KeyStroke> keys, MappingOwner owner) {
    for (KeyStroke key : keys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        if (!injector.getApplication().isOctopusEnabled() ||
            !(key.getKeyCode() == KeyEvent.VK_ESCAPE && key.getModifiers() == 0) &&
            !(key.getKeyCode() == KeyEvent.VK_ENTER && key.getModifiers() == 0)) {
          getRequiredShortcutKeys().add(new RequiredShortcut(key, owner));
        }
      }
    }
  }

  public static @NotNull ShortcutSet toShortcutSet(@NotNull Collection<RequiredShortcut> requiredShortcuts) {
    final List<Shortcut> shortcuts = new ArrayList<>();
    for (RequiredShortcut key : requiredShortcuts) {
      shortcuts.add(new KeyboardShortcut(key.getKeyStroke(), null));
    }
    return new CustomShortcutSet(shortcuts.toArray(new Shortcut[0]));
  }

  private static @NotNull List<Pair<Set<MappingMode>, MappingInfo>> getKeyMappingRows(@NotNull Set<? extends MappingMode> modes,
                                                                                      @NotNull List<? extends KeyStroke> prefix) {
    // Some map commands set a mapping for more than one mode (e.g. `map` sets for Normal, Visual, Select and
    // Op-pending). Vim treats this as a single mapping, and when listing all maps only lists it once, with the
    // appropriate mode indicator(s) in the first column (NVO is a space char). If the lhs mapping is changed or cleared
    // for one of the modes, the original mapping is still a single map for the remaining modes, and the indicator
    // changes. E.g. `map foo bar` followed by `sunmap foo` would result in `nox foo bar` in the output to `map`.
    // Vim doesn't do automatic grouping - `nmap foo bar` followed by `omap foo bar` and `vmap foo bar` would result in
    // 3 lines in the output to `map` - one for `n`, one for `o` and one for `v`.
    // We store mappings separately per mode (to simplify lookup, especially when matching prefixes), but want to have
    // the same behaviour as Vim in map output. So we store the original modes with the mapping and check they're still
    // valid as we collect output
    final List<Pair<Set<MappingMode>, MappingInfo>> rows = new ArrayList<>();
    final MultiMap<List<? extends KeyStroke>, Set<MappingMode>> multiModeMappings = MultiMap.create();
    final List<KeyStroke> fromKeys = new ArrayList<>();

    for (MappingMode mode : modes) {
      final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);

      // Vim includes mappings for each key in the prefix, where appropriate. That is, it doesn't just all mappings that
      // are descendants of the prefix, but includes the mappings for each key in the prefix as well.
      // E.g. `foo` will include mappings for `f` and `fo`, as well as any mappings that are descendants of `foo`.
      final Iterator<KeyMappingEntry> iterator = mapping.getAll(prefix, true).iterator();
      while (iterator.hasNext()) {
        final KeyMappingEntry entry = iterator.next();
        final MappingInfo mappingInfo = entry.getMappingInfo();

        final Set<@NotNull MappingMode> originalModes = mappingInfo.getOriginalModes();
        if (originalModes.size() == 1) {
          rows.add(new Pair<>(originalModes, mappingInfo));
        }
        else {
          entry.collectPath(fromKeys);
          if (!multiModeMappings.get(fromKeys).contains(originalModes)) {
            multiModeMappings.putValue(new ArrayList<>(fromKeys), originalModes);
            rows.add(new Pair<>(getModesForMapping(fromKeys, originalModes), mappingInfo));
          }
        }
      }
    }
    rows.sort(Comparator.comparing(Pair<Set<MappingMode>, MappingInfo>::getSecond));
    return rows;
  }

  private static @NotNull Set<MappingMode> getModesForMapping(@NotNull List<? extends KeyStroke> keyStrokes,
                                                              @NotNull Set<MappingMode> originalMappingModes) {
    final Set<MappingMode> actualModes = EnumSet.noneOf(MappingMode.class);
    for (MappingMode mode : originalMappingModes) {
      final MappingInfo mappingInfo = VimPlugin.getKey().getKeyMapping(mode).get(keyStrokes);
      if (mappingInfo != null && mappingInfo.getOriginalModes() == originalMappingModes) {
        actualModes.add(mode);
      }
    }
    return actualModes;
  }

  private static @NotNull @NonNls String getModesStringCode(@NotNull Set<MappingMode> modes) {
    if (modes.equals(MappingMode.IC)) return "!";
    if (modes.equals(MappingMode.NVO)) return " ";
    if (modes.equals(MappingMode.C)) return "c";
    if (modes.equals(MappingMode.I)) return "i";
    //if (modes.equals(MappingMode.L)) return "l";

    // The following modes are concatenated
    String mode = "";
    if (modes.containsAll(MappingMode.N)) mode += "n";
    if (modes.containsAll(MappingMode.O)) mode += "o";

    if (modes.containsAll(MappingMode.V)) {
      mode += "v";
    }
    else {
      if (modes.containsAll(MappingMode.X)) mode += "x";
      if (modes.containsAll(MappingMode.S)) mode += "s";
    }
    return mode;
  }

  private @NotNull List<AnAction> getActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    results.addAll(getLocalActions(component, keyStroke));
    results.addAll(getKeymapActions(keyStroke));
    return results;
  }

  @Override
  public @NotNull List<NativeAction> getActions(@NotNull VimEditor editor, @NotNull KeyStroke keyStroke) {
    return getActions(((IjVimEditor)editor).getEditor().getComponent(), keyStroke).stream().map(IjNativeAction::new)
      .collect(toList());
  }

  private static @NotNull List<AnAction> getLocalActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final KeyboardShortcut keyStrokeShortcut = new KeyboardShortcut(keyStroke, null);
    for (Component c = component; c != null; c = c.getParent()) {
      if (c instanceof JComponent) {
        final List<AnAction> actions = ActionUtil.getActions((JComponent)c);
        for (AnAction action : actions) {
          if (action instanceof VimShortcutKeyAction || action == VimShortcutKeyAction.getInstance()) {
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

  private static @NotNull List<AnAction> getKeymapActions(@NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    for (String id : keymap.getActionIds(keyStroke)) {
      final AnAction action = ActionManager.getInstance().getAction(id);

      // EmptyAction is used to reserve a shortcut, but can't be executed. Code can ask for an action by ID and
      // use its shortcut(s) to dynamically register a new action on a component in the UI hierarchy. It's not
      // useful for our needs here - we'll pick up the real action when we get local actions.
      if (action != null && !(action instanceof EmptyAction)) {
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
  public boolean showKeyMappings(@NotNull Set<? extends MappingMode> modes,
                                 @NotNull List<? extends KeyStroke> prefix,
                                 @NotNull VimEditor editor) {
    List<Pair<Set<MappingMode>, MappingInfo>> rows = getKeyMappingRows(modes, prefix);

    final StringBuilder builder = new StringBuilder();
    for (Pair<Set<MappingMode>, MappingInfo> row : rows) {
      MappingInfo mappingInfo = row.getSecond();
      builder.append(StringsKt.padEnd(getModesStringCode(row.getFirst()), 3, ' '));
      builder.append(
        StringsKt.padEnd(VimInjectorKt.getInjector().getParser().toKeyNotation(mappingInfo.getFromKeys()) + " ", 12,
                         ' '));
      builder.append(mappingInfo.isRecursive() ? " " : "*");  // Or `&` if script-local mappings being recursive
      builder.append(" ");  // Should be `@` if it's a buffer-local mapping
      builder.append(mappingInfo.getPresentableString());
      builder.append("\n");
    }

    if (builder.isEmpty()) {
      builder.append("No mapping found");
    }

    VimOutputPanel outputPanel = injector.getOutputPanel()
      .getOrCreate(editor, injector.getExecutionContextManager().getEditorExecutionContext(editor));
    outputPanel.addText(builder.toString(), true);
    outputPanel.show();
    return true;
  }

  @Override
  public @Nullable Character getChar(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    KeyStroke stroke = VimExtensionFacade.inputKeyStroke(ijEditor);
    char keyChar = stroke.getKeyChar();
    if (keyChar == KeyEvent.CHAR_UNDEFINED) {
      return null;
    }
    else {
      return keyChar;
    }
  }
}
