/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group;

import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.key.Shortcut;
import com.maddyhome.idea.vim.key.*;
import kotlin.text.StringsKt;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static com.maddyhome.idea.vim.helper.StringHelper.toKeyNotation;

/**
 * @author vlan
 */
public class KeyGroup {
  private static final String SHORTCUT_CONFLICTS_ELEMENT = "shortcut-conflicts";
  private static final String SHORTCUT_CONFLICT_ELEMENT = "shortcut-conflict";
  private static final String OWNER_ATTRIBUTE = "owner";
  private static final String TEXT_ELEMENT = "text";

  @NotNull private final Map<KeyStroke, ShortcutOwner> shortcutConflicts = new LinkedHashMap<>();
  @NotNull private final Set<KeyStroke> requiredShortcutKeys = new HashSet<>();
  @NotNull private final HashMap<MappingMode, RootNode> keyRoots = new HashMap<>();
  @NotNull private final Map<MappingMode, KeyMapping> keyMappings = new HashMap<>();
  @Nullable private OperatorFunction operatorFunction = null;

  void registerRequiredShortcutKeys(@NotNull Editor editor) {
    EventFacade.getInstance().registerCustomShortcutSet(VimShortcutKeyAction.getInstance(),
                                                        toShortcutSet(requiredShortcutKeys), editor.getComponent());
  }

  public void registerShortcutsForLookup(@NotNull LookupImpl lookup) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(requiredShortcutKeys),
                                 lookup.getComponent(), lookup);
  }

  void unregisterShortcutKeys(@NotNull Editor editor) {
    EventFacade.getInstance().unregisterCustomShortcutSet(VimShortcutKeyAction.getInstance(), editor.getComponent());
  }

  public boolean showKeyMappings(@NotNull Set<MappingMode> modes, @NotNull Editor editor) {
    final List<MappingInfo> rows = getKeyMappingRows(modes);
    final StringBuilder builder = new StringBuilder();
    for (MappingInfo row : rows) {
      builder.append(StringsKt.padEnd(getModesStringCode(row.getMappingModes()), 2, ' '));
      builder.append(" ");
      builder.append(StringsKt.padEnd(toKeyNotation(row.getFromKeys()), 11, ' '));
      builder.append(" ");
      builder.append(row.isRecursive() ? " " : "*");
      builder.append(" ");
      final List<KeyStroke> toKeys = row.getToKeys();
      final VimExtensionHandler extensionHandler = row.getExtensionHandler();
      if (toKeys != null) {
        builder.append(toKeyNotation(toKeys));
      }
      else if (extensionHandler != null) {
        builder.append("call ");
        builder.append(extensionHandler.getClass().getCanonicalName());
      }
      else {
        builder.append("<Unknown>");
      }
      builder.append("\n");
    }
    ExOutputModel.getInstance(editor).output(builder.toString());
    return true;
  }

  public void putKeyMapping(@NotNull Set<MappingMode> modes, @NotNull List<KeyStroke> fromKeys,
                            @Nullable List<KeyStroke> toKeys, @Nullable VimExtensionHandler extensionHandler,
                            boolean recursive) {
    for (MappingMode mode : modes) {
      final KeyMapping mapping = getKeyMapping(mode);
      mapping.put(EnumSet.of(mode), fromKeys, toKeys, extensionHandler, recursive);
    }
    final int oldSize = requiredShortcutKeys.size();
    for (KeyStroke key : fromKeys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        requiredShortcutKeys.add(key);
      }
    }
    if (requiredShortcutKeys.size() != oldSize) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        unregisterShortcutKeys(editor);
        registerRequiredShortcutKeys(editor);
      }
    }
  }

  @Nullable
  public OperatorFunction getOperatorFunction() {
    return operatorFunction;
  }

  public void setOperatorFunction(@NotNull OperatorFunction function) {
    operatorFunction = function;
  }

  public void saveData(@NotNull Element element) {
    final Element conflictsElement = new Element(SHORTCUT_CONFLICTS_ELEMENT);
    for (Map.Entry<KeyStroke, ShortcutOwner> entry : shortcutConflicts.entrySet()) {
      final ShortcutOwner owner = entry.getValue();
      if (owner != ShortcutOwner.UNDEFINED) {
        final Element conflictElement = new Element(SHORTCUT_CONFLICT_ELEMENT);
        conflictElement.setAttribute(OWNER_ATTRIBUTE, owner.getName());
        final Element textElement = new Element(TEXT_ELEMENT);
        StringHelper.setSafeXmlText(textElement, entry.getKey().toString());
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
        }
        catch (IllegalArgumentException ignored) {
        }
        final Element textElement = conflictElement.getChild(TEXT_ELEMENT);
        if (textElement != null) {
          final String text = StringHelper.getSafeXmlText(textElement);
          if (text != null) {
            final KeyStroke keyStroke = KeyStroke.getKeyStroke(text);
            if (keyStroke != null) {
              shortcutConflicts.put(keyStroke, owner);
            }
          }
        }
      }
    }
  }

  @NotNull
  public List<AnAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
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
    return actions;
  }

  @NotNull
  public Map<KeyStroke, ShortcutOwner> getShortcutConflicts() {
    final Set<KeyStroke> requiredShortcutKeys = this.requiredShortcutKeys;
    final Map<KeyStroke, ShortcutOwner> savedConflicts = getSavedShortcutConflicts();
    final Map<KeyStroke, ShortcutOwner> results = new HashMap<>();
    for (KeyStroke keyStroke : requiredShortcutKeys) {
      if (!VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
        final List<AnAction> conflicts = getKeymapConflicts(keyStroke);
        if (!conflicts.isEmpty()) {
          final ShortcutOwner owner = savedConflicts.get(keyStroke);
          results.put(keyStroke, owner != null ? owner : ShortcutOwner.UNDEFINED);
        }
      }
    }
    return results;
  }

  @NotNull
  public Map<KeyStroke, ShortcutOwner> getSavedShortcutConflicts() {
    return shortcutConflicts;
  }

  @NotNull
  public KeyMapping getKeyMapping(@NotNull MappingMode mode) {
    KeyMapping mapping = keyMappings.get(mode);
    if (mapping == null) {
      mapping = new KeyMapping();
      keyMappings.put(mode, mapping);
    }
    return mapping;
  }

  public void resetKeyMappings() {
    keyMappings.clear();
  }

  /**
   * Returns the root of the key mapping for the given mapping mode
   *
   * @param mappingMode The mapping mode
   * @return The key mapping tree root
   */
  public RootNode getKeyRoot(@NotNull MappingMode mappingMode) {
    RootNode res = keyRoots.get(mappingMode);
    // Create the root node if one doesn't exist yet for this mode
    if (res == null) {
      res = new RootNode();
      keyRoots.put(mappingMode, res);
    }

    return res;
  }

  /**
   * Registers a shortcut that is handled by KeyHandler#handleKey directly, rather than by an action
   *
   * <p>
   * Digraphs are handled directly by KeyHandler#handleKey instead of via an action, but we need to still make sure the
   * shortcuts are registered, or the key handler won't see them
   * </p>
   * @param shortcut The shortcut to register
   */
  public void registerShortcutWithoutAction(Shortcut shortcut) {
    registerRequiredShortcut(shortcut);
  }

  public void registerCommandAction(@NotNull EditorActionHandlerBase commandAction) {
    for (List<KeyStroke> keyStrokes : commandAction.getKeyStrokesSet()) {
      final KeyStroke[] keys = registerRequiredShortcut(new Shortcut(keyStrokes.toArray(new KeyStroke[0])));
      registerAction(commandAction.getMappingModes(), commandAction, commandAction.getType(),
                     commandAction.getFlags(), keys, commandAction.getArgumentType());
    }
  }

  private KeyStroke[] registerRequiredShortcut(@NotNull Shortcut shortcut) {
    final KeyStroke[] keys = shortcut.getKeys();
    for (KeyStroke key : keys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        requiredShortcutKeys.add(key);
      }
    }
    return keys;
  }

  private void registerAction(@NotNull Set<MappingMode> mappingModes,
                              EditorActionHandlerBase action,
                              @NotNull Command.Type cmdType,
                              EnumSet<CommandFlags> cmdFlags,
                              @NotNull KeyStroke[] keys,
                              @NotNull Argument.Type argType) {
    for (MappingMode mappingMode : mappingModes) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        identityChecker = new HashMap<>();
        checkIdentity(mappingMode, action.getId(), keys);
      }
      Node node = getKeyRoot(mappingMode);
      final int len = keys.length;
      // Add a child for each keystroke in the shortcut for this action
      for (int i = 0; i < len; i++) {
        if (node instanceof ParentNode) {
          final ParentNode base = (ParentNode)node;
          node = addNode(base, action, cmdType, cmdFlags, keys[i], argType, i == len - 1);
        }
      }
    }
  }

  private void checkIdentity(MappingMode mappingMode, String actName, KeyStroke[] keys) {
    Set<List<KeyStroke>> keySets = identityChecker.computeIfAbsent(mappingMode, k -> new HashSet<>());
    if (keySets.contains(Arrays.asList(keys))) throw new RuntimeException("This keymap already exists: " + mappingMode + " keys: " + Arrays.asList(keys) + " action:" + actName);
    keySets.add(Arrays.asList(keys));
  }

  private Map<MappingMode, Set<List<KeyStroke>>> identityChecker;

  @NotNull
  private Node addNode(@NotNull ParentNode base,
                       EditorActionHandlerBase action,
                       @NotNull Command.Type cmdType,
                       EnumSet<CommandFlags> cmdFlags,
                       @NotNull KeyStroke key,
                       @NotNull Argument.Type argType,
                       boolean last) {
    // Lets get the actual action for the supplied action name
    Node node = base.getChild(key);
    // Is this the first time we have seen this character at this point in the tree?
    if (node == null) {
      // If this is the last keystroke in the shortcut, and there is no argument, add a command node
      if (last && argType == Argument.Type.NONE) {
        node = new CommandNode(key, action, cmdType, cmdFlags);
      }
      // If this are more keystrokes in the shortcut or there is an argument, add a branch node
      else {
        node = new BranchNode(key, cmdFlags);
      }

      base.addChild(node, key);
    }

    // If this is the last keystroke in the shortcut and we have an argument, add an argument node
    if (last && node instanceof BranchNode && argType != Argument.Type.NONE) {
      ArgumentNode arg = new ArgumentNode(action, cmdType, argType, cmdFlags);
      ((BranchNode)node).addChild(arg, BranchNode.ARGUMENT);
    }

    if (base instanceof BranchNode) {
      // All flags of a child should be added to parent
      // Otherwise set of this flags will differ for different initialization orders
      ((BranchNode)base).getFlags().addAll(cmdFlags);
    }

    return node;
  }

  @NotNull
  private static ShortcutSet toShortcutSet(@NotNull Collection<KeyStroke> keyStrokes) {
    final List<com.intellij.openapi.actionSystem.Shortcut> shortcuts = new ArrayList<>();
    for (KeyStroke key : keyStrokes) {
      shortcuts.add(new KeyboardShortcut(key, null));
    }
    return new CustomShortcutSet(shortcuts.toArray(new com.intellij.openapi.actionSystem.Shortcut[0]));
  }

  @NotNull
  private static List<MappingInfo> getKeyMappingRows(@NotNull Set<MappingMode> modes) {
    final Map<ImmutableList<KeyStroke>, Set<MappingMode>> actualModes = new HashMap<>();
    for (MappingMode mode : modes) {
      final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
      for (List<KeyStroke> fromKeys : mapping) {
        final ImmutableList<KeyStroke> key = ImmutableList.copyOf(fromKeys);
        final Set<MappingMode> value = actualModes.get(key);
        final Set<MappingMode> newValue;
        if (value != null) {
          newValue = new HashSet<>(value);
          newValue.add(mode);
        }
        else {
          newValue = EnumSet.of(mode);
        }
        actualModes.put(key, newValue);
      }
    }
    final List<MappingInfo> rows = new ArrayList<>();
    for (Map.Entry<ImmutableList<KeyStroke>, Set<MappingMode>> entry : actualModes.entrySet()) {
      final ArrayList<KeyStroke> fromKeys = new ArrayList<>(entry.getKey());
      final Set<MappingMode> mappingModes = entry.getValue();
      if (!mappingModes.isEmpty()) {
        final MappingMode mode = mappingModes.iterator().next();
        final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
        final MappingInfo mappingInfo = mapping.get(fromKeys);
        if (mappingInfo != null) {
          rows.add(new MappingInfo(mappingModes, mappingInfo.getFromKeys(), mappingInfo.getToKeys(),
                                   mappingInfo.getExtensionHandler(), mappingInfo.isRecursive()));
        }
      }
    }
    Collections.sort(rows);
    return rows;
  }

  @NotNull
  private static String getModesStringCode(@NotNull Set<MappingMode> modes) {
    if (modes.equals(MappingMode.NVO)) {
      return "";
    }
    else if (modes.contains(MappingMode.INSERT)) {
      return "i";
    }
    else if (modes.contains(MappingMode.NORMAL)) {
      return "n";
    }
    // TODO: Add more codes
    return "";
  }

  @NotNull
  public List<AnAction> getActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    results.addAll(getLocalActions(component, keyStroke));
    results.addAll(getKeymapActions(keyStroke));
    return results;
  }

  @NotNull
  private static List<AnAction> getLocalActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final KeyboardShortcut keyStrokeShortcut = new KeyboardShortcut(keyStroke, null);
    for (Component c = component; c != null; c = c.getParent()) {
      if (c instanceof JComponent) {
        final List<AnAction> actions = ActionUtil.getActions((JComponent)c);
        for (AnAction action : actions) {
          if (action instanceof VimShortcutKeyAction) {
            continue;
          }
          final com.intellij.openapi.actionSystem.Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
          for (com.intellij.openapi.actionSystem.Shortcut shortcut : shortcuts) {
            if (shortcut.isKeyboard() && shortcut.startsWith(keyStrokeShortcut) && !results.contains(action)) {
              results.add(action);
            }
          }
        }
      }
    }
    return results;
  }

  @NotNull
  private static List<AnAction> getKeymapActions(@NotNull KeyStroke keyStroke) {
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
}
