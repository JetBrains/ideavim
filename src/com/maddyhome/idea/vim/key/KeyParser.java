package com.maddyhome.idea.vim.key;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003 Rick Maddy
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.handler.key.EditorKeyHandler;
import java.util.HashMap;
import javax.swing.KeyStroke;

/**
 * The key parser creates a tree of key sequences with terminals represnting complete keystroke sequences mapped to
 * specific actions. Arguments also act as terminals represents a complete command that requires more keystrokes as
 * an argument.
 * <p>
 * There are several trees. Each tree represents a valid set of keystroke sequences for a given mode in Vim. These
 * modes include:
 * <ul>
 * <li>Normal - The mode in which you enter typical commands such as movement and delete</li>
 * <li>Visual - The mode used to highlight portions of text</li>
 * <li>Insert - The mode where you actually enter text into the editor</li>
 * <li>Operator Pending - This mode is entered after an operator has been entered. Arguments then follow</li>
 * <li>Command Line - The mode for entering ex commands</li>
 * </ul>A
 * Several convenience methods are provided for building the key mapping trees. The mappings supplied to all the
 * <code>registerAction</code> methods are combinations of the five mapping constants. The action names supplied
 * must be valid action ids registered with Idea. These can be built in actions supplied with Idea or custom actions
 * supplied with the plugin. All the custom Vim Plugin actions are listed in the plugin.xml file.
 */
public class KeyParser
{
    /** Special flag used for any mappings involving operators */
    public static final int FLAG_OP_PEND = 256;
    public static final int FLAG_MULTIKEY_UNDO = 512;
    public static final int FLAG_EXPECT_MORE = 1024;

    /** Indicates this key mapping applies to Normal mode */
    public static final int MAPPING_NORMAL = 1;
    /** Indicates this key mapping applies to Visual mode */
    public static final int MAPPING_VISUAL = 2;
    /** Indicates this key mapping applies to Operator Pending mode */
    public static final int MAPPING_OP_PEND = 4;
    /** Indicates this key mapping applies to Insert mode */
    public static final int MAPPING_INSERT = 8;
    /** Indicates this key mapping applies to Command Line mode */
    public static final int MAPPING_CMD_LINE = 16;
    private static final int MAPPING_CNT = 5;

    /** Helper value for the typical key mapping that works in Normal, Visual, and Operator Pending modes */
    public static final int MAPPING_NVO = MAPPING_NORMAL | MAPPING_VISUAL | MAPPING_OP_PEND;

    /**
     * Returns the singleton instance of this key parser
     * @return The singleton instance
     */
    public static KeyParser getInstance()
    {
        if (instance == null)
        {
            instance = new KeyParser();
        }

        return instance;
    }

    public static void setupActionHandler(String ideaActName, String vimActName, KeyStroke stroke)
    {
        ActionManager amgr = ActionManager.getInstance();
        EditorAction iaction = (EditorAction)amgr.getAction(ideaActName);
        EditorActionHandler handler = iaction.getHandler();
        if (vimActName != null)
        {
            EditorAction vaction = (EditorAction)amgr.getAction(vimActName);
            vaction.setupHandler(handler);
        }
        iaction.setupHandler(new EditorKeyHandler(handler, stroke));
    }

    /**
     * Creates the key parser
     */
    private KeyParser()
    {
        logger.debug("KeyParser ctr");
    }

    /**
     * Returns the root of the key mapping for the given mapping mode
     * @param mode The mapping mode
     * @return The key mapping tree root
     */
    public RootNode getKeyRoot(int mode)
    {
        RootNode res = (RootNode)keyRoots.get(new Integer(mode));
        // Create the root node if one doesn't exist yet for this mode
        if (res == null)
        {
            res = new RootNode();
            keyRoots.put(new Integer(mode), res);
        }

        return res;
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcut is applicable to
     * @param actName The action the shortcut will execute
     * @param cmdType The type of the command
     * @param shortcut The shortcut to map to the action
     */
    public void registerAction(int mapping, String actName, int cmdType, Shortcut shortcut)
    {
        registerAction(mapping, actName, cmdType, new Shortcut[] { shortcut });
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcut is applicable to
     * @param actName The action the shortcut will execute
     * @param cmdType The type of the command
     * @param cmdFlags Any special flags associated with this command
     * @param shortcut The shortcut to map to the action
     */
    public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut shortcut)
    {
        registerAction(mapping, actName, cmdType, cmdFlags, new Shortcut[] { shortcut });
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcut is applicable to
     * @param actName The action the shortcut will execute
     * @param cmdType The type of the command
     * @param shortcut The shortcut to map to the action
     * @param argType The type of argument required by the actions
     */
    public void registerAction(int mapping, String actName, int cmdType, Shortcut shortcut, int argType)
    {
        registerAction(mapping, actName, cmdType, new Shortcut[] { shortcut }, argType);
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcut is applicable to
     * @param actName The action the shortcut will execute
     * @param cmdType The type of the command
     * @param cmdFlags Any special flags associated with this command
     * @param shortcut The shortcut to map to the action
     * @param argType The type of argument required by the actions
     */
    public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut shortcut, int argType)
    {
        registerAction(mapping, actName, cmdType, cmdFlags, new Shortcut[] { shortcut }, argType);
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcuts are applicable to
     * @param actName The action the shortcuts will execute
     * @param cmdType The type of the command
     * @param shortcuts The shortcuts to map to the action
     */
    public void registerAction(int mapping, String actName, int cmdType, Shortcut[] shortcuts)
    {
        registerAction(mapping, actName, cmdType, 0, shortcuts);
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcuts are applicable to
     * @param actName The action the shortcuts will execute
     * @param cmdType The type of the command
     * @param shortcuts The shortcuts to map to the action
     * @param argType The type of argument required by the actions
     */
    public void registerAction(int mapping, String actName, int cmdType, Shortcut[] shortcuts, int argType)
    {
        registerAction(mapping, actName, cmdType, 0, shortcuts, argType);
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcuts are applicable to
     * @param actName The action the shortcuts will execute
     * @param cmdType The type of the command
     * @param cmdFlags Any special flags associated with this command
     * @param shortcuts The shortcuts to map to the action
     */
    public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut[] shortcuts)
    {
        registerAction(mapping, actName, cmdType, cmdFlags, shortcuts, Argument.NONE);
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the shortcuts are applicable to
     * @param actName The action the shortcuts will execute
     * @param cmdType The type of the command
     * @param cmdFlags Any special flags associated with this command
     * @param shortcuts The shortcuts to map to the action
     * @param argType The type of argument required by the actions
     */
    public void registerAction(int mapping, String actName, int cmdType, int cmdFlags, Shortcut[] shortcuts, int argType)
    {
        for (int i = 0; i < shortcuts.length; i++)
        {
            registerAction(mapping, actName, cmdType, cmdFlags, shortcuts[i].getKeys(), argType);
        }
    }

    /**
     * Registers the action
     * @param mapping The set of mappings the keystrokes are applicable to
     * @param actName The action the keystrokes will execute
     * @param cmdType The type of the command
     * @param cmdFlags Any special flags associated with this command
     * @param keys The keystrokes to map to the action
     * @param argType The type of argument required by the actions
     */
    private void registerAction(int mapping, String actName, int cmdType, int cmdFlags, KeyStroke[] keys, int argType)
    {
        // Look through all the possible mappings and see which ones apply to this action
        int map = 1;
        for (int m = 0; m < MAPPING_CNT; m++)
        {
            if ((mapping & map) != 0)
            {
                Node node = getKeyRoot(map);
                int len = keys.length;
                // Add a child for each keystroke in the shortcut for this action
                for (int i = 0; i < len; i++)
                {
                    ParentNode base = (ParentNode)node;

                    node = addNode(base, actName, cmdType, cmdFlags, keys[i], argType, i == len - 1);
                }
            }

            map <<= 1;
        }
    }

    /**
     * Adds a new node to the tree
     * @param base The specific node in the mapping tree this keystroke gets added to
     * @param actName The action the keystroke will execute
     * @param cmdType The type of the command
     * @param cmdFlags Any special flags associated with this command
     * @param key The keystroke to map to the action
     * @param argType The type of argument required by the action
     */
    private Node addNode(ParentNode base, String actName, int cmdType, int cmdFlags, KeyStroke key, int argType, boolean last)
    {
        // Lets get the actual action for the supplied action name
        ActionManager aMgr = ActionManager.getInstance();
        AnAction action = aMgr.getAction(actName);
        if (action == null)
        {
            // Programmer error
            logger.error("Unknown action " + actName);
        }
        Node node = base.getChild(key);
        // Is this the first time we have seen this character at this point in the tree?
        if (node == null)
        {
            // If this is the last keystroke in the shortcut, and there is no argument, add a command node
            if (last && argType == Argument.NONE)
            {
                node = new CommandNode(key, action, cmdType, cmdFlags);
            }
            // If this are more keystrokes in the shortcut or there is an argument, add a branch node
            else
            {
                node = new BranchNode(key);
            }

            base.addChild(node, key);
        }

        // If this is the last keystroke in the shortcut and we have an argument, add an argument node
        if (last && node instanceof BranchNode && argType != Argument.NONE)
        {
            ArgumentNode arg = new ArgumentNode(action, cmdType, argType, cmdFlags);
            ((BranchNode)node).addChild(arg, BranchNode.ARGUMENT);
        }

        return node;
    }

    public String toString()
    {
        StringBuffer res = new StringBuffer();

        res.append("KeyParser=[");
        res.append("roots=[");
        res.append(keyRoots);
        res.append("]");

        return res.toString();
    }

    private HashMap keyRoots = new HashMap();

    private static KeyParser instance;

    private static Logger logger = Logger.getInstance(KeyParser.class.getName());
}
