package com.maddyhome.idea.vim.key;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

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
import com.maddyhome.idea.vim.handler.key.EditorKeyHandler;

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

  /**
   * This is called each time the plugin is enabled. This goes through the list of keystrokes that the plugin needs
   * such as Ctrl-A through Ctrl-Z, F1, and others. Each keystroke is checks for existing IDEA actions using the
   * keystroke and the keystroke is removed from the IDEA action. The keystroke is then added to the internal
   * VimKeyHandler action.
   */
  /*
  public void setupShortcuts()
  {
      logger.debug("setupShortcuts");
      logger.debug("conflicts=" + conflicts);

      keymap = KeymapManager.getInstance().getActiveKeymap();
      // Loop through each of the keystrokes the plugin needs to take ownership of.
      Iterator keys = conflicts.keySet().iterator();
      while (keys.hasNext())
      {
          KeyStroke keyStroke = (KeyStroke)keys.next();
          logger.debug("keyStroke=" + keyStroke);
          // Get all the IDEA actions that use this keystroke. Could be 0 or more.
          KeyConflict conf = (KeyConflict)conflicts.get(keyStroke);

          if (!conf.isPluginWins()) continue;

          setupShortcut(conf, keyStroke);
      }

      keymap.addShortcutChangeListener(keymapListener);

      logger.debug("setup conflicts=" + conflicts);
  }

  private void setupShortcut(KeyConflict conf, KeyStroke keyStroke)
  {
      HashMap actions = conf.getIdeaActions();
      logger.debug("actions=" + actions);
      String[] ids = keymap.getActionIds(keyStroke);
      // For each existing IDEA action we need to remove shortcut.
      boolean added = false;
      for (int i = 0; i < ids.length; i++)
      {
          String id = ids[i];
          logger.debug("id=" + id);
          // Get the list of shortcuts for the IDEA action and find the one that matches the current keystroke
          com.intellij.openapi.actionSystem.Shortcut[] cuts = keymap.getShortcuts(id);
          for (int j = 0; j < cuts.length; j++)
          {
              com.intellij.openapi.actionSystem.Shortcut cut = cuts[j];
              logger.debug("cut=" + cut.getClass().getName());
              if (cut instanceof KeyboardShortcut)
              {
                  if (((KeyboardShortcut)cut).getFirstKeyStroke().equals(keyStroke))
                  {
                      keymap.removeShortcut(id, cut);
                      // Save off the position of the shortcut so it can be put back in the same place.
                      conf.putIdeaAction(id, j);
                      if (!added)
                      {
                          keymap.addShortcut(KEY_HANDLER, cut);
                          added = true;
                      }
                      logger.debug("removed " + cut.getClass().getName() + " from " + id + " at " + j + " and added to VimKeyHandler");
                  }
              }
          }
      }
  }
  */

  /**
   * This is called each time the plugin is disabled. This processes all the keystrokes originally removed from their
   * original IDEA action and are put back into place.
   */
  /*
  public void resetShortcuts()
  {
      logger.debug("resetShortcuts");

      keymap.removeShortcutChangeListener(keymapListener);

      // Get each of the hijacked keystrokes we stole from IDEA and put them back in their original place.
      com.intellij.openapi.actionSystem.Shortcut[] cuts = keymap.getShortcuts(KEY_HANDLER);
      for (int i = 0; i < cuts.length; i++)
      {
          com.intellij.openapi.actionSystem.Shortcut cut = cuts[i];
          if (cut instanceof KeyboardShortcut)
          {
              KeyboardShortcut ks = (KeyboardShortcut)cut;
              KeyStroke keyStroke = ks.getFirstKeyStroke();
              logger.debug("keyStroke=" + keyStroke);
              // Get the list of IDEA actions that originally had this keystroke - if any
              KeyConflict conf = (KeyConflict)conflicts.get(keyStroke);
              resetConflict(conf, cut);
          }
      }

      logger.debug("reset conflicts=" + conflicts);
  }

  private void resetConflict(KeyConflict conf, com.intellij.openapi.actionSystem.Shortcut cut)
  {
      // Remove the shortcut from the special plugin handler
      keymap.removeShortcut(KEY_HANDLER, cut);

      HashMap actions = conf.getIdeaActions();
      Iterator iter = actions.keySet().iterator();
      while (iter.hasNext())
      {
          String actionId = (String)iter.next();
          int keyPos = ((Integer)actions.get(actionId)).intValue();
          logger.debug("actionId=" + actionId);
          logger.debug("keyPos=" + keyPos);

          if (keyPos == -1) continue;

          conf.resetIdeaAction(actionId);
          // Put back the removed shortcut. But we need to "insert" it in the same place it was so the menus
          // will show the shortcuts. Example - Undo has by default two shortcuts - Ctrl-Z and Alt-Backspace.
          // When the plugin is disabled the Undo menu shows Ctrl-Z. When the plugin is enabled we remove
          // Ctrl-Z and the Undo menu shows Alt-Backspace. When the plugin is disabled again, we need to be
          // sure Ctrl-Z is put back before Alt-Backspace or else the Undo menu will continue to show
          // Alt-Backspace even after we add back Ctrl-Z.
          com.intellij.openapi.actionSystem.Shortcut[] acuts = keymap.getShortcuts(actionId);
          logger.debug("There are " + acuts.length + " shortcuts");
          keymap.removeAllActionShortcuts(actionId);
          for (int k = 0, l = 0; k < acuts.length + 1; k++)
          {
              if (k == keyPos)
              {
                  keymap.addShortcut(actionId, cut);
              }
              else
              {
                  keymap.addShortcut(actionId, acuts[l++]);
              }
          }
      }
  }
  */
  public void setupActionHandler(String ideaActName, String vimActName) {
    if (logger.isDebugEnabled()) logger.debug("vimActName=" + vimActName);

    ActionManager amgr = ActionManager.getInstance();
    AnAction vaction = amgr.getAction(vimActName);
    if (vaction instanceof DelegateAction) {
      amgr.unregisterAction(vimActName);
    }
    setupActionHandler(ideaActName, vaction);
  }

  public void setupActionHandler(String ideaActName, AnAction vaction) {
    if (logger.isDebugEnabled()) logger.debug("ideaActName=" + ideaActName);

    ActionManager amgr = ActionManager.getInstance();
    AnAction iaction = amgr.getAction(ideaActName);
    if (iaction == null) return;  // ignore actions which aren't available in RubyMine
    if (vaction instanceof DelegateAction) {
      DelegateAction daction = (DelegateAction)vaction;
      daction.setOrigAction(iaction);

      //Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
      //com.intellij.openapi.actionSystem.Shortcut[] icuts = keymap.getShortcuts(ideaActName);
      //keymap.removeAllActionShortcuts(ideaActName);

      amgr.unregisterAction(ideaActName);

      amgr.registerAction(ideaActName, vaction);

      //for (int i = 0; i < icuts.length; i++)
      //{
      //    keymap.addShortcut(ideaActName, icuts[i]);
      //}
    }

    amgr.registerAction("Orig" + ideaActName, iaction);
  }

  public void setupActionHandler(String ideaActName, String vimActName, KeyStroke stroke) {
    setupActionHandler(ideaActName, vimActName, stroke, false);
  }

  public void setupActionHandler(String ideaActName, String vimActName, KeyStroke stroke, boolean special) {
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

  /*
  public void removePossibleConflict(KeyStroke stroke)
  {
      if (conflicts.containsKey(stroke))
      {
          conflicts.remove(stroke);
      }
  }
  public void addPossibleConflict(KeyStroke stroke, String pluginActionId)
  {
      if (!conflicts.containsKey(stroke))
      {
          KeyConflict conf = new KeyConflict(stroke);
          conf.addPluginAction(pluginActionId);
          if (ideaWins.contains(stroke))
          {
              conf.setPluginWins(false);
          }

          conflicts.put(stroke, conf);

          keymap = KeymapManager.getInstance().getActiveKeymap();
          String[] ids = keymap.getActionIds(stroke);
          for (int i = 0; i < ids.length; i++)
          {
              String id = ids[i];
              conf.addIdeaAction(id);
          }
      }
      else
      {
          KeyConflict conf = (KeyConflict)conflicts.get(stroke);
          conf.addPluginAction(pluginActionId);
      }
  }
  */

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
  public void registerAction(int mapping, String actName, int cmdType) {
    registerAction(mapping, actName, cmdType, 0);
  }

  public void registerAction(int mapping, String actName, int cmdType, int cmdFlags) {
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
  public void registerAction(int mapping, String actName, int cmdType, Shortcut shortcut) {
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
  public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut shortcut) {
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
  public void registerAction(int mapping, String actName, int cmdType, Shortcut shortcut, int argType) {
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
  public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut shortcut, int argType) {
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
  public void registerAction(int mapping, String actName, int cmdType, Shortcut[] shortcuts) {
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
  public void registerAction(int mapping, String actName, int cmdType, Shortcut[] shortcuts, int argType) {
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
  public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut[] shortcuts) {
    registerAction(mapping, actName, cmdType, cmdFlags, shortcuts, Argument.NONE);
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
  public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut[] shortcuts, int argType) {
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
  private void registerAction(int mapping, String actName, int cmdType, int cmdFlags, KeyStroke[] keys, int argType) {
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
  private Node addNode(ParentNode base, String actName, int cmdType, int cmdFlags, KeyStroke key, int argType, boolean last) {
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
      if (last && argType == Argument.NONE) {
        node = new CommandNode(key, actName, action, cmdType, cmdFlags);
      }
      // If this are more keystrokes in the shortcut or there is an argument, add a branch node
      else {
        node = new BranchNode(key, cmdFlags);
      }

      base.addChild(node, key);
    }

    // If this is the last keystroke in the shortcut and we have an argument, add an argument node
    if (last && node instanceof BranchNode && argType != Argument.NONE) {
      ArgumentNode arg = new ArgumentNode(actName, action, cmdType, argType, cmdFlags);
      ((BranchNode)node).addChild(arg, BranchNode.ARGUMENT);
    }

    return node;
  }

  public String toString() {
    StringBuffer res = new StringBuffer();

    res.append("KeyParser=[");
    res.append("roots=[");
    res.append(keyRoots);
    res.append("]");

    return res.toString();
  }

  /*
  public void setChoices(HashSet choices)
  {
      keymap.removeShortcutChangeListener(keymapListener);
      // Reset choices not currently in ideawins
      Iterator iter = choices.iterator();
      while (iter.hasNext())
      {
          KeyStroke keyStroke = (KeyStroke)iter.next();
          if (ideaWins.contains(keyStroke)) continue;

          KeyConflict conf = (KeyConflict)conflicts.get(keyStroke);
          com.intellij.openapi.actionSystem.Shortcut[] cuts = keymap.getShortcuts(KEY_HANDLER);
          for (int i = 0; i < cuts.length; i++)
          {
              if (cuts[i] instanceof KeyboardShortcut)
              {
                  if (((KeyboardShortcut)cuts[i]).getFirstKeyStroke().equals(keyStroke))
                  {
                      resetConflict(conf, cuts[i]);
                  }
              }
          }
          conf.setPluginWins(false);
      }

      // Setup ideawins not in choices anymore
      iter = ideaWins.iterator();
      while (iter.hasNext())
      {
          KeyStroke keyStroke = (KeyStroke)iter.next();
          if (choices.contains(keyStroke)) continue;

          KeyConflict conf = (KeyConflict)conflicts.get(keyStroke);
          setupShortcut(conf, keyStroke);
          conf.setPluginWins(true);
      }

      ideaWins = choices;
      keymap.addShortcutChangeListener(keymapListener);
  }

  public HashSet getChoices()
  {
      return ideaWins;
  }

  public HashMap getConflicts()
  {
      return conflicts;
  }

  public void readData(Element element)
  {
      Element confElem = element.getChild("conflicts");
      if (confElem != null)
      {
          List keyList = confElem.getChildren("keys");
          for (int i = 0; i < keyList.size(); i++)
          {
              Element key = (Element)keyList.get(i);
              Attribute chAttr = key.getAttribute("char");
              if (chAttr != null)
              {
                  char ch = key.getAttributeValue("char").charAt(0);
                  ideaWins.add(KeyStroke.getKeyStroke(ch));
              }
              else
              {
                  int code = Integer.parseInt(key.getAttributeValue("code"));
                  int mods = Integer.parseInt(key.getAttributeValue("mods"));
                  boolean rel = Boolean.getBoolean(key.getAttributeValue("release"));
                  ideaWins.add(KeyStroke.getKeyStroke(code, mods, rel));
              }
          }
      }

      com.intellij.openapi.actionSystem.Shortcut[] cuts = keymap.getShortcuts(KEY_HANDLER);
      Element originals = element.getChild("originals");
      if (originals != null)
      {
          List origList = originals.getChildren("original");
          for (int i = 0; i < origList.size(); i++)
          {
              Element original = (Element)origList.get(i);
              Attribute chAttr = original.getAttribute("char");
              KeyStroke key;
              if (chAttr != null)
              {
                  char ch = original.getAttributeValue("char").charAt(0);
                  key = KeyStroke.getKeyStroke(ch);
              }
              else
              {
                  int code = Integer.parseInt(original.getAttributeValue("code"));
                  int mods = Integer.parseInt(original.getAttributeValue("mods"));
                  boolean rel = Boolean.getBoolean(original.getAttributeValue("release"));
                  key = KeyStroke.getKeyStroke(code, mods, rel);
              }

              for (int c = 0; c < cuts.length; c++)
              {
                  if (cuts[c] instanceof KeyboardShortcut)
                  {
                      if (((KeyboardShortcut)cuts[c]).getFirstKeyStroke().equals(key))
                      {
                          keymap.removeShortcut(KEY_HANDLER, cuts[c]);

                          //ArrayList ids = new ArrayList();
                          List actList = original.getChildren("action");
                          for (int j = 0; j < actList.size(); j++)
                          {
                              Element action = (Element)actList.get(j);
                              String id = action.getText();
                              int pos = Integer.parseInt(action.getAttributeValue("pos"));
                              if (pos == -1) continue;
                              //ids.add(id);

                              com.intellij.openapi.actionSystem.Shortcut[] acuts = keymap.getShortcuts(id);
                              logger.debug("There are " + acuts.length + " shortcuts");
                              keymap.removeAllActionShortcuts(id);
                              for (int k = 0, l = 0; k < acuts.length + 1; k++)
                              {
                                  if (k == pos)
                                  {
                                      keymap.addShortcut(id, cuts[c]);
                                  }
                                  else
                                  {
                                      keymap.addShortcut(id, acuts[l++]);
                                  }
                              }
                          }
                      }
                  }
              }
          }
      }
  }

  public void saveData(Element element)
  {
      Element confElem = new Element("conflicts");
      Iterator iter = ideaWins.iterator();
      while (iter.hasNext())
      {
          KeyStroke key = (KeyStroke)iter.next();
          Element keyElem = new Element("keys");
          if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
          {
              keyElem.setAttribute("char", Character.toString(key.getKeyChar()));
          }
          else
          {
              keyElem.setAttribute("code", Integer.toString(key.getKeyCode()));
              keyElem.setAttribute("mods", Integer.toString(key.getModifiers()));
              keyElem.setAttribute("release", Boolean.toString(key.isOnKeyRelease()));
          }
          confElem.addContent(keyElem);
      }
      element.addContent(confElem);

      Element originals = new Element("originals");
      iter = conflicts.keySet().iterator();
      while (iter.hasNext())
      {
          KeyStroke keyStroke = (KeyStroke)iter.next();
          KeyConflict conf = (KeyConflict)conflicts.get(keyStroke);
          if (conf.hasConflict())
          {
              Element original = new Element("original");
              if (keyStroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
              {
                  original.setAttribute("char", Character.toString(keyStroke.getKeyChar()));
              }
              else
              {
                  original.setAttribute("code", Integer.toString(keyStroke.getKeyCode()));
                  original.setAttribute("mods", Integer.toString(keyStroke.getModifiers()));
                  original.setAttribute("release", Boolean.toString(keyStroke.isOnKeyRelease()));
              }

              HashMap acts = conf.getIdeaActions();
              Iterator aIter = acts.keySet().iterator();
              while (aIter.hasNext())
              {
                  String name = (String)aIter.next();
                  if (name.equals(KEY_HANDLER)) continue;

                  Integer pos = (Integer)acts.get(name);
                  Element action = new Element("action");
                  action.setText(name);
                  action.setAttribute("pos", pos.toString());
                  original.addContent(action);
              }
              originals.addContent(original);
          }
      }

      element.addContent(originals);
  }
  */

  /**
   * Unfortunately this isn't called when the user makes a shortcut change using the Keymap Settings dialog.
   * This "bug" has been reported against IDEA 5.0.2 (3542).
   * Due to this bug many changes a user makes with the settings dialog will basically be lost.
   */
  private class KeyChangeListener implements Keymap.Listener {
    // Possible changes: (1 or more)
    // 1) User removed a shortcut that was never a conflict.
    //    Nothing to do or even check on this - who cares.
    //
    // 2) User removed a shortcut that was a conflict.
    //    Remove the action from the conflict list for the key.
    //
    // 3) User put back a shortcut removed by this plugin on same action.
    //    This only happens if previous setting was for plugin to win so change key to 'idea wins' and reset
    //    conflict. Shortcut will already be on idea action. May or may not still be on 'VimKeyHandler'. Put
    //    shortcut back on any other idea actions that used it too.
    //
    // 4) User added a shortcut to a different action and shortcut was a conflict.
    //    If key is currently 'idea wins' then simply add shortcut to key's list of conflicts.
    //    If key is currently 'plugin wins' then put shortcut reset conflict. Change key to 'idea wins'.
    //    Shortcut will already be on new idea action. May or may not still be on 'VimKeyHandler'. Add
    //    action to key's list of conflicts.
    //
    // 5) User added a shortcut to a different action and shortcut is now a conflict.
    //    No prior idea actions used this key. Add action to key's list of conflicts. Mark key as 'idea wins'.
    //    Action will already have shortcut. Need to remove shortcut from 'VimKeyHandler' (may already be gone).
    //
    // 6) User added a shortcut to a different action and shortcut still isn't a conflict.
    //    Nothing to do.
    public void onShortcutChanged(String actionId) {
      /* TODO - this is untested code due to the bug.
      // Get the newly updated list of shortcuts for this action
      com.intellij.openapi.actionSystem.Shortcut[] cuts = keymap.getShortcuts(actionId);

      // Check the current master conflict list to see if there is a conflict for a key no longer associated
      // with this action.
      Iterator iter = conflicts.keySet().iterator();
      while (iter.hasNext())
      {
          KeyStroke key = (KeyStroke)iter.next();
          KeyConflict conf = (KeyConflict)conflicts.get(key);
          // We found a key with this action
          if (conf.getIdeaActions().containsKey(actionId))
          {
              boolean found = false;
              // Now check to see if any of the new shortcuts are already known about.
              for (int i = 0; i < cuts.length; i++)
              {
                  com.intellij.openapi.actionSystem.Shortcut cut = cuts[i];
                  if (cut instanceof KeyboardShortcut)
                  {
                      // OK - we already knew about this shortcut
                      if (((KeyboardShortcut)cut).getFirstKeyStroke().equals(key))
                      {
                          found = true;
                          break;
                      }
                  }
              }
              // This key isn't listed anymore. The user must have removed it as part of this change
              // so we need to remove it from our conflict list. That should be all. What ever other
              // conflict may exist for this key can stay as is.
              if (!found)
              {
                  conf.removeIdeaAction(actionId);
              }
          }
      }

      // Now we need to go through each of the shortcuts in the updated list.
      for (int i = 0; i < cuts.length; i++)
      {
          com.intellij.openapi.actionSystem.Shortcut cut = cuts[i];
          if (cut instanceof KeyboardShortcut)
          {
              KeyStroke key = ((KeyboardShortcut)cut).getFirstKeyStroke();
              KeyConflict conf = (KeyConflict)conflicts.get(key);
              // This is a new conflict that didn't exist for this action before
              if (!conf.getIdeaActions().containsKey(actionId))
              {
                  // Assume that the user wants this new shortcut to override what ever it normally does
                  // in the plugin (otherwise they wouldn't have added it to the Idea action).
                  // First thing is to reset the shortcut by restoring the shortcut to any and all Idea
                  // actions we took it away from.
                  if (conf.isPluginWins())
                  {
                      resetConflict(conf, cut);
                  }

                  // Add this as a new action for the key and mark it as Idea winning over the plugin.
                  conf.addIdeaAction(actionId);
                  conf.setPluginWins(false);
                  ideaWins.add(key);
              }
              else
              {
                  if (conf.isPluginWins())
                  {
                      // The shortcut existed before but was marked as 'plugin wins'. If we are here the
                      // user must have used the Keymap settings to put the shortcut back to the idea
                      // action. So lets mark it as 'idea wins' and reset the shortcut on the actions.
                      conf.resetIdeaAction(actionId);
                      resetConflict(conf, cut);
                      conf.setPluginWins(false);
                      ideaWins.add(key);
                  }
                  else
                  {
                      // The shortcut existed before and was already setup as 'idea wins' so this can't be
                      // a changed shortcut - nothing to do.
                  }
              }
          }
      }
      */
    }
  }

  private HashMap<Integer, RootNode> keyRoots = new HashMap<Integer, RootNode>();
  //private HashMap conflicts = new HashMap();
  //private HashSet ideaWins = new HashSet();
  //private Keymap keymap;
  //private KeyChangeListener keymapListener = new KeyChangeListener();

  private static KeyParser instance;

  private static Logger logger = Logger.getInstance(KeyParser.class.getName());
  //private static final String KEY_HANDLER = "VimKeyHandler";
}
