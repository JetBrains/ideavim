/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.key;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * The key parser creates a tree of key sequences with terminals representing complete keystroke sequences mapped to
 * specific actions. Arguments also act as terminals represents a complete command that requires more keystrokes as
 * an argument.
 * <p/>
 * There are several trees. Each tree represents a valid set of keystroke sequences for a given mode in Vim. These
 * modes include:
 * <ul>
 * <li>Normal - The mode in which you enter typical commands such as movement and delete</li>
 * <li>Visual - The mode used to highlight portions of text</li>
 * <li>Insert - The mode where you actually enter text into the editor</li>
 * <li>Operator Pending - This mode is entered after an operator has been entered. Arguments then follow</li>
 * <li>Command Line - The mode for entering ex commands</li>
 * </ul>
 * Several convenience methods are provided for building the key mapping trees. The mappings supplied to all the
 * <code>registerAction</code> methods are combinations of the five mapping constants. The action names supplied
 * must be valid action ids registered with Idea. These can be built in actions supplied with Idea or custom actions
 * supplied with the plugin. All the custom Vim Plugin actions are listed in the plugin.xml file.
 */
public class KeyParser {
  private static KeyParser instance;
  private static Logger logger = Logger.getInstance(KeyParser.class.getName());

  @NotNull private Set<KeyStroke> requiredShortcutKeys = new HashSet<KeyStroke>();
  @NotNull private HashMap<MappingMode, RootNode> keyRoots = new HashMap<MappingMode, RootNode>();

  public static KeyParser getInstance() {
    if (instance == null) {
      instance = new KeyParser();
    }
    return instance;
  }

  private KeyParser() {
  }

  @NotNull
  public static List<AnAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    final Keymap keymap = keymapManager.getActiveKeymap();
    final KeyboardShortcut shortcut = new KeyboardShortcut(keyStroke, null);
    final Map<String, ArrayList<KeyboardShortcut>> conflicts = keymap.getConflicts("", shortcut);
    final List<AnAction> actions = new ArrayList<AnAction>();
    for (String actionId : conflicts.keySet()) {
      final AnAction action = ActionManagerEx.getInstanceEx().getAction(actionId);
      if (action != null) {
        actions.add(action);
      }
    }
    return actions;
  }

  @NotNull
  public static Map<KeyStroke, ShortcutOwner> getShortcutConflicts() {
    final KeyParser keyParser = getInstance();
    final Set<KeyStroke> requiredShortcutKeys = keyParser.getRequiredShortcutKeys();
    final Map<KeyStroke, ShortcutOwner> savedConflicts = VimPlugin.getSavedShortcutConflicts();
    final Map<KeyStroke, ShortcutOwner> results = new HashMap<KeyStroke, ShortcutOwner>();
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
  @Override
  public String toString() {
    return "KeyParser=[roots=[" + keyRoots + "]";
  }

  @NotNull
  public Set<KeyStroke> getRequiredShortcutKeys() {
    return requiredShortcutKeys;
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

  public void registerCommandAction(@NotNull VimCommandAction commandAction, @NotNull String actionId) {
    final List<Shortcut> shortcuts = new ArrayList<Shortcut>();
    for (List<KeyStroke> keyStrokes : commandAction.getKeyStrokesSet()) {
      shortcuts.add(new Shortcut(keyStrokes.toArray(new KeyStroke[keyStrokes.size()])));
    }
    registerAction(commandAction.getMappingModes(), actionId, commandAction.getType(), commandAction.getFlags(),
                   shortcuts.toArray(new Shortcut[shortcuts.size()]), commandAction.getArgumentType());
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, Shortcut shortcut) {
    registerAction(mappingModes, actName, cmdType, new Shortcut[]{shortcut});
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, Shortcut shortcut) {
    registerAction(mappingModes, actName, cmdType, cmdFlags, new Shortcut[]{shortcut});
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, Shortcut shortcut,
                             @NotNull Argument.Type argType) {
    registerAction(mappingModes, actName, cmdType, new Shortcut[]{shortcut}, argType);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, Shortcut shortcut,
                             @NotNull Argument.Type argType) {
    registerAction(mappingModes, actName, cmdType, cmdFlags, new Shortcut[]{shortcut}, argType);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, @NotNull Shortcut[] shortcuts) {
    registerAction(mappingModes, actName, cmdType, 0, shortcuts);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, @NotNull Shortcut[] shortcuts,
                             @NotNull Argument.Type argType) {
    registerAction(mappingModes, actName, cmdType, 0, shortcuts, argType);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull Shortcut[] shortcuts) {
    registerAction(mappingModes, actName, cmdType, cmdFlags, shortcuts, Argument.Type.NONE);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull Shortcut[] shortcuts,
                             @NotNull Argument.Type argType) {
    for (Shortcut shortcut : shortcuts) {
      final KeyStroke[] keys = shortcut.getKeys();
      for (KeyStroke key : keys) {
        if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
          requiredShortcutKeys.add(key);
        }
      }
      registerAction(mappingModes, actName, cmdType, cmdFlags, keys, argType);
    }
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  private void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull KeyStroke[] keys,
                              @NotNull Argument.Type argType) {
    for (MappingMode mappingMode : mappingModes) {
      Node node = getKeyRoot(mappingMode);
      final int len = keys.length;
      // Add a child for each keystroke in the shortcut for this action
      for (int i = 0; i < len; i++) {
        if (node instanceof ParentNode) {
          final ParentNode base = (ParentNode)node;
          node = addNode(base, actName, cmdType, cmdFlags, keys[i], argType, i == len - 1);
        }
      }
    }
  }

  @Nullable
  private Node addNode(@NotNull ParentNode base, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull KeyStroke key,
                       @NotNull Argument.Type argType, boolean last) {
    // Lets get the actual action for the supplied action name
    ActionManager aMgr = ActionManager.getInstance();
    AnAction action = aMgr.getAction(actName);
    if (action == null) {
      // Programmer error
      logger.error("Unknown action " + actName);
    }

    //addPossibleConflict(key, actName);

    Node node = base.getChild(key);
    // Is this the first time we have seen this character at this point in the tree?
    if (node == null) {
      // If this is the last keystroke in the shortcut, and there is no argument, add a command node
      if (last && argType == Argument.Type.NONE) {
        node = new CommandNode(key, actName, action, cmdType, cmdFlags);
      }
      // If this are more keystrokes in the shortcut or there is an argument, add a branch node
      else {
        node = new BranchNode(key, cmdFlags);
      }

      base.addChild(node, key);
    }

    // If this is the last keystroke in the shortcut and we have an argument, add an argument node
    if (last && node instanceof BranchNode && argType != Argument.Type.NONE) {
      ArgumentNode arg = new ArgumentNode(actName, action, cmdType, argType, cmdFlags);
      ((BranchNode)node).addChild(arg, BranchNode.ARGUMENT);
    }

    return node;
  }
}
