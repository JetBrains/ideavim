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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.maddyhome.idea.vim.action.DelegateAction;
import com.maddyhome.idea.vim.action.PassThruDelegateAction;
import com.maddyhome.idea.vim.action.PassThruDelegateEditorAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.handler.key.EditorKeyHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The key parser creates a tree of key sequences with terminals represnting complete keystroke sequences mapped to
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
  /**
   * Indicates this key mapping applies to Normal mode
   */
  public static final int MAPPING_NORMAL = 1;
  /**
   * Indicates this key mapping applies to Visual mode
   */
  public static final int MAPPING_VISUAL = 2;
  /**
   * Indicates this key mapping applies to Operator Pending mode
   */
  public static final int MAPPING_OP_PEND = 4;
  /**
   * Indicates this key mapping applies to Insert mode
   */
  public static final int MAPPING_INSERT = 8;
  /**
   * Indicates this key mapping applies to Command Line mode
   */
  public static final int MAPPING_CMD_LINE = 16;
  private static final int MAPPING_CNT = 5;

  /**
   * Helper value for the typical key mapping that works in Normal, Visual, and Operator Pending modes
   */
  public static final int MAPPING_NVO = MAPPING_NORMAL | MAPPING_VISUAL | MAPPING_OP_PEND;
  public static final int MAPPING_ALL = MAPPING_NVO | MAPPING_INSERT | MAPPING_CMD_LINE;

  /**
   * Returns the singleton instance of this key parser
   *
   * @return The singleton instance
   */
  public static KeyParser getInstance() {
    if (instance == null) {
      instance = new KeyParser();
    }

    return instance;
  }

  public void setupActionHandler(@NotNull String ideaActName, @NotNull String vimActName) {
    if (logger.isDebugEnabled()) logger.debug("vimActName=" + vimActName);

    ActionManager amgr = ActionManager.getInstance();
    AnAction vaction = amgr.getAction(vimActName);
    if (vaction instanceof DelegateAction) {
      amgr.unregisterAction(vimActName);
    }
    setupActionHandler(ideaActName, vaction);
  }

  public void setupActionHandler(@NotNull String ideaActName, @NotNull AnAction vaction) {
    if (logger.isDebugEnabled()) logger.debug("ideaActName=" + ideaActName);

    ActionManager amgr = ActionManager.getInstance();
    AnAction iaction = amgr.getAction(ideaActName);
    if (iaction == null) return;  // ignore actions which aren't available in RubyMine
    if (vaction instanceof DelegateAction) {
      DelegateAction daction = (DelegateAction)vaction;
      daction.setOrigAction(iaction);

      amgr.unregisterAction(ideaActName);

      amgr.registerAction(ideaActName, vaction);
    }

    amgr.registerAction("Orig" + ideaActName, iaction);
  }

  public void setupActionHandler(@NotNull String ideaActName, String vimActName, @NotNull KeyStroke stroke) {
    setupActionHandler(ideaActName, vimActName, stroke, false);
  }

  public void setupActionHandler(@NotNull String ideaActName, @Nullable String vimActName, @NotNull KeyStroke stroke, boolean special) {
    if (logger.isDebugEnabled()) logger.debug("setupActionHandler for " + ideaActName + " and " + vimActName + " for " + stroke);
    ActionManager amgr = ActionManager.getInstance();
    AnAction action = amgr.getAction(ideaActName);
    if (action instanceof EditorAction) {
      if (logger.isDebugEnabled()) logger.debug(ideaActName + " is an EditorAction");
      EditorAction iaction = (EditorAction)action;
      EditorActionHandler handler = iaction.getHandler();
      if (vimActName != null) {
        EditorAction vaction = (EditorAction)amgr.getAction(vimActName);
        vaction.setupHandler(handler);
      }

      iaction.setupHandler(new EditorKeyHandler(handler, stroke, special));
    }

    //removePossibleConflict(stroke);
  }

  /**
   * Creates the key parser
   */
  private KeyParser() {
    logger.debug("KeyParser ctr");
    //keymap = KeymapManager.getInstance().getActiveKeymap();
  }

  /**
   * Returns the root of the key mapping for the given mapping mode
   *
   * @param mode The mapping mode
   * @return The key mapping tree root
   */
  public RootNode getKeyRoot(int mode) {
    RootNode res = keyRoots.get(new Integer(mode));
    // Create the root node if one doesn't exist yet for this mode
    if (res == null) {
      res = new RootNode();
      keyRoots.put(mode, res);
    }

    return res;
  }

  /**
   * Registers the action
   *
   * @param mapping The set of mappings the shortcut is applicable to
   * @param actName The action the shortcut will execute
   * @param cmdType The type of the command
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType) {
    registerAction(mapping, actName, cmdType, 0);
  }

  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags) {
    String ideaName = actName.substring(3);
    ActionManager amgr = ActionManager.getInstance();
    if (amgr.getAction(ideaName) == null) {
      logger.info("No registered action " + ideaName);
      return;
    }

    Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    com.intellij.openapi.actionSystem.Shortcut[] cuts = keymap.getShortcuts(ideaName);
    ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
    for (com.intellij.openapi.actionSystem.Shortcut cut : cuts) {
      if (cut instanceof KeyboardShortcut) {
        KeyStroke keyStroke = ((KeyboardShortcut)cut).getFirstKeyStroke();
        Shortcut shortcut = new Shortcut(keyStroke);
        shortcuts.add(shortcut);
      }
    }

    registerAction(mapping, actName, cmdType, cmdFlags, shortcuts.toArray(new Shortcut[]{}));
    KeyStroke firstStroke = null;
    for (int i = 0; i < shortcuts.size(); i++) {
      Shortcut cut = shortcuts.get(i);
      //removePossibleConflict(cut.getKeys()[0]);
      if (i == 0) {
        firstStroke = cut.getKeys()[0];
      }
    }

    AnAction iaction = amgr.getAction(ideaName);
    AnAction vaction = amgr.getAction(actName);
    if (vaction instanceof DelegateAction) {
      DelegateAction daction = (DelegateAction)vaction;
      daction.setOrigAction(iaction);
    }

    if (iaction instanceof EditorAction) {
      EditorAction ea = (EditorAction)iaction;
      setupActionHandler(ideaName, new PassThruDelegateEditorAction(firstStroke, ea.getHandler()));
    }
    else {
      setupActionHandler(ideaName, new PassThruDelegateAction(firstStroke));
    }
  }

  /**
   * Registers the action
   *
   * @param mapping  The set of mappings the shortcut is applicable to
   * @param actName  The action the shortcut will execute
   * @param cmdType  The type of the command
   * @param shortcut The shortcut to map to the action
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, Shortcut shortcut) {
    registerAction(mapping, actName, cmdType, new Shortcut[]{shortcut});
  }

  /**
   * Registers the action
   *
   * @param mapping  The set of mappings the shortcut is applicable to
   * @param actName  The action the shortcut will execute
   * @param cmdType  The type of the command
   * @param cmdFlags Any special flags associated with this command
   * @param shortcut The shortcut to map to the action
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, Shortcut shortcut) {
    registerAction(mapping, actName, cmdType, cmdFlags, new Shortcut[]{shortcut});
  }

  /**
   * Registers the action
   *
   * @param mapping  The set of mappings the shortcut is applicable to
   * @param actName  The action the shortcut will execute
   * @param cmdType  The type of the command
   * @param shortcut The shortcut to map to the action
   * @param argType  The type of argument required by the actions
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, Shortcut shortcut,
                             @NotNull Argument.Type argType) {
    registerAction(mapping, actName, cmdType, new Shortcut[]{shortcut}, argType);
  }

  /**
   * Registers the action
   *
   * @param mapping  The set of mappings the shortcut is applicable to
   * @param actName  The action the shortcut will execute
   * @param cmdType  The type of the command
   * @param cmdFlags Any special flags associated with this command
   * @param shortcut The shortcut to map to the action
   * @param argType  The type of argument required by the actions
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, Shortcut shortcut,
                             @NotNull Argument.Type argType) {
    registerAction(mapping, actName, cmdType, cmdFlags, new Shortcut[]{shortcut}, argType);
  }

  /**
   * Registers the action
   *
   * @param mapping   The set of mappings the shortcuts are applicable to
   * @param actName   The action the shortcuts will execute
   * @param cmdType   The type of the command
   * @param shortcuts The shortcuts to map to the action
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, @NotNull Shortcut[] shortcuts) {
    registerAction(mapping, actName, cmdType, 0, shortcuts);
  }

  /**
   * Registers the action
   *
   * @param mapping   The set of mappings the shortcuts are applicable to
   * @param actName   The action the shortcuts will execute
   * @param cmdType   The type of the command
   * @param shortcuts The shortcuts to map to the action
   * @param argType   The type of argument required by the actions
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, @NotNull Shortcut[] shortcuts,
                             @NotNull Argument.Type argType) {
    registerAction(mapping, actName, cmdType, 0, shortcuts, argType);
  }

  /**
   * Registers the action
   *
   * @param mapping   The set of mappings the shortcuts are applicable to
   * @param actName   The action the shortcuts will execute
   * @param cmdType   The type of the command
   * @param cmdFlags  Any special flags associated with this command
   * @param shortcuts The shortcuts to map to the action
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull Shortcut[] shortcuts) {
    registerAction(mapping, actName, cmdType, cmdFlags, shortcuts, Argument.Type.NONE);
  }

  /**
   * Registers the action
   *
   * @param mapping   The set of mappings the shortcuts are applicable to
   * @param actName   The action the shortcuts will execute
   * @param cmdType   The type of the command
   * @param cmdFlags  Any special flags associated with this command
   * @param shortcuts The shortcuts to map to the action
   * @param argType   The type of argument required by the actions
   */
  public void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull Shortcut[] shortcuts,
                             @NotNull Argument.Type argType) {
    for (Shortcut shortcut : shortcuts) {
      registerAction(mapping, actName, cmdType, cmdFlags, shortcut.getKeys(), argType);
    }
  }

  /**
   * Registers the action
   *
   * @param mapping  The set of mappings the keystrokes are applicable to
   * @param actName  The action the keystrokes will execute
   * @param cmdType  The type of the command
   * @param cmdFlags Any special flags associated with this command
   * @param keys     The keystrokes to map to the action
   * @param argType  The type of argument required by the actions
   */
  private void registerAction(int mapping, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull KeyStroke[] keys,
                              @NotNull Argument.Type argType) {
    // Look through all the possible mappings and see which ones apply to this action
    int map = 1;
    for (int m = 0; m < MAPPING_CNT; m++) {
      if ((mapping & map) != 0) {
        Node node = getKeyRoot(map);
        int len = keys.length;
        // Add a child for each keystroke in the shortcut for this action
        for (int i = 0; i < len; i++) {
          ParentNode base = (ParentNode)node;

          node = addNode(base, actName, cmdType, cmdFlags, keys[i], argType, i == len - 1);
        }
      }

      map <<= 1;
    }
  }

  /**
   * Adds a new node to the tree
   *
   * @param base     The specific node in the mapping tree this keystroke gets added to
   * @param actName  The action the keystroke will execute
   * @param cmdType  The type of the command
   * @param cmdFlags Any special flags associated with this command
   * @param key      The keystroke to map to the action
   * @param argType  The type of argument required by the action
   * @param last     True if last
   * @return Node
   */
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

  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();

    res.append("KeyParser=[");
    res.append("roots=[");
    res.append(keyRoots);
    res.append("]");

    return res.toString();
  }


  @NotNull private HashMap<Integer, RootNode> keyRoots = new HashMap<Integer, RootNode>();

  private static KeyParser instance;

  private static Logger logger = Logger.getInstance(KeyParser.class.getName());
}
