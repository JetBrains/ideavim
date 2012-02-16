package com.maddyhome.idea.vim;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.helper.DelegateCommandListener;
import com.maddyhome.idea.vim.helper.DigraphSequence;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.key.*;
import com.maddyhome.idea.vim.option.Options;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * This handlers every keystroke that the user can argType except those that are still valid hotkeys for various Idea
 * actions. This is a singleton.
 */
public class KeyHandler {
  /**
   * Returns a reference to the singleton instance of this class
   *
   * @return A reference to the singleton
   */
  public static KeyHandler getInstance() {
    if (instance == null) {
      instance = new KeyHandler();
    }

    return instance;
  }

  /**
   * Creates an instance
   */
  private KeyHandler() {
    reset(null);
  }

  /**
   * Sets the original key handler
   *
   * @param origHandler The original key handler
   */
  public void setOriginalHandler(TypedActionHandler origHandler) {
    this.origHandler = origHandler;
  }

  /**
   * Gets the original key handler
   *
   * @return The orginal key handler
   */
  public TypedActionHandler getOriginalHandler() {
    return origHandler;
  }

  /**
   * This is the main key handler for the Vim plugin. Every keystroke not handled directly by Idea is sent here for
   * processing.
   *
   * @param editor  The editor the key was typed into
   * @param key     The keystroke typed by the user
   * @param context The data context
   */
  public void handleKey(Editor editor, KeyStroke key, DataContext context) {
    // All the editor actions should be performed with top level editor!!!
    // Be careful: all the EditorActionHandler implementation should correctly process InjectedEditors
    editor = InjectedLanguageUtil.getTopLevelEditor(editor);
    logger.debug("handleKey " + key);
    CommandState editorState = CommandState.getInstance(editor);
    boolean isRecording = editorState.isRecording();
    boolean shouldRecord = true;
    for (int loop = 0; loop < 2; loop++) {
      // If this is a "regular" character keystroke, get the character
      char chKey = key.getKeyChar() == KeyEvent.CHAR_UNDEFINED ? 0 : key.getKeyChar();

      if ((editorState.getMode() == CommandState.MODE_COMMAND || mode == STATE_COMMAND) &&
          (key.getKeyCode() == KeyEvent.VK_ESCAPE ||
           (key.getKeyCode() == KeyEvent.VK_C && (key.getModifiers() & KeyEvent.CTRL_MASK) != 0) ||
           (key.getKeyCode() == '[' && (key.getModifiers() & KeyEvent.CTRL_MASK) != 0))) {
        if (mode != STATE_COMMAND && count == 0 && currentArg == Argument.NONE && currentCmd.size() == 0 &&
            CommandGroups.getInstance().getRegister().getCurrentRegister() == RegisterGroup.REGISTER_DEFAULT) {
          if (key.getKeyCode() == KeyEvent.VK_ESCAPE) {
            KeyHandler.executeAction("VimEditorEscape", context);
            //getOriginalHandler().execute(editor, key.getKeyChar(), context);
          }
          VimPlugin.indicateError();
        }

        reset(editor);
      }
      // At this point the user must be typing in a command. Most commands can be preceeded by a number. Let's
      // check if a number can be entered at this point, and if so, did the user send us a digit.
      else if ((editorState.getMode() == CommandState.MODE_COMMAND ||
                editorState.getMode() == CommandState.MODE_VISUAL) &&
               mode == STATE_NEW_COMMAND && currentArg != Argument.CHARACTER && currentArg != Argument.DIGRAPH &&
               Character.isDigit(chKey) &&
               (count != 0 || chKey != '0')) {
        // Update the count
        count = count * 10 + (chKey - '0');
        logger.debug("count now " + count);
      }
      // Pressing delete while entering a count "removes" the last digit entered
      else if ((editorState.getMode() == CommandState.MODE_COMMAND ||
                editorState.getMode() == CommandState.MODE_VISUAL) &&
               mode == STATE_NEW_COMMAND && currentArg != Argument.CHARACTER && currentArg != Argument.DIGRAPH &&
               key.getKeyCode() == KeyEvent.VK_DELETE && count != 0) {
        // "Remove" the last digit sent to us
        count /= 10;
        logger.debug("count now " + count);
      }
      // If we got this far the user is entering a command or supplying an argument to an entered command.
      // First let's check to see if we are at the point of expecting a single character argument to a command.
      else if (currentArg == Argument.CHARACTER) {
        logger.debug("currentArg is Character");
        // We are expecting a character argument - is this a regular character the user typed?
        // Some special keys can be handled as character arguments - let's check for them here.
        if (chKey == 0) {
          switch (key.getKeyCode()) {
            case KeyEvent.VK_TAB:
              chKey = '\t';
              break;
            case KeyEvent.VK_ENTER:
              chKey = '\n';
              break;
          }
        }

        if (chKey != 0) {
          // Create the character argument, add it to the current command, and signal we are ready to process
          // the command
          Argument arg = new Argument(chKey);
          Command cmd = currentCmd.peek();
          cmd.setArgument(arg);
          mode = STATE_READY;
        }
        else {
          // Oops - this isn't a valid character argument
          mode = STATE_BAD_COMMAND;
        }
      }
      // If we are this far - sheesh, then the user must be entering a command or a non-single-character argument
      // to an entered command. Let's figure out which it is
      else {
        // For debugging purposes we track the keys entered for this command
        keys.add(key);
        if (logger.isDebugEnabled()) {
          logger.debug("keys now " + keys);
        }

        // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
        // return the node matching this keystroke
        Node node = editorState.getCurrentNode().getChild(key);

        if (digraph == null && !(node instanceof CommandNode) && DigraphSequence.isDigraphStart(key)) {
          digraph = new DigraphSequence();
        }
        if (digraph != null) {
          DigraphSequence.DigraphResult res = digraph.processKey(key, editor, context);
          switch (res.getResult()) {
            case DigraphSequence.DigraphResult.RES_OK:
              return;
            case DigraphSequence.DigraphResult.RES_BAD:
              digraph = null;
              return;
            case DigraphSequence.DigraphResult.RES_DONE:
              if (currentArg == Argument.DIGRAPH) {
                currentArg = Argument.CHARACTER;
              }
              key = res.getStroke();
              digraph = null;
              continue;
          }

          logger.debug("digraph done");
        }

        // If this is a branch node we have entered only part of a multikey command
        if (node instanceof BranchNode) {
          logger.debug("branch node");
          BranchNode bnode = (BranchNode)node;
          // Flag that we aren't allowing any more count digits (unless it's OK)
          if ((bnode.getFlags() & Command.FLAG_ALLOW_MID_COUNT) == 0) {
            mode = STATE_COMMAND;
          }
          editorState.setCurrentNode(bnode);

          ArgumentNode arg = (ArgumentNode)((BranchNode)editorState.getCurrentNode()).getArgumentNode();
          if (arg != null) {
            if (editorState.isRecording() && (arg.getFlags() & Command.FLAG_NO_ARG_RECORDING) != 0) {
              handleKey(editor, KeyStroke.getKeyStroke(' '), context);
            }

            if (arg.getArgType() == Argument.EX_STRING) {
              CommandGroups.getInstance().getProcess().startSearchCommand(editor, context, count, chKey);
              mode = STATE_NEW_COMMAND;
              currentArg = Argument.EX_STRING;
              editorState.pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
            }
          }
        }
        // If this is a command node the user has entered a valid key sequence of a known command
        else if (node instanceof CommandNode) {
          logger.debug("command node");
          // If all does well we are ready to process this command
          mode = STATE_READY;
          CommandNode cmdNode = (CommandNode)node;
          // Did we just get the completed sequence for a motion command argument?
          if (currentArg == Argument.MOTION) {
            // We have been expecting a motion argument - is this one?
            if (cmdNode.getCmdType() == Command.MOTION) {
              // Create the motion command and add it to the stack
              Command cmd = new Command(count, cmdNode.getActionId(), cmdNode.getAction(),
                                        cmdNode.getCmdType(), cmdNode.getFlags());
              cmd.setKeys(keys);
              currentCmd.push(cmd);
            }
            else if (cmdNode.getCmdType() == Command.RESET) {
              currentCmd.clear();
              Command cmd = new Command(1, cmdNode.getActionId(), cmdNode.getAction(),
                                        cmdNode.getCmdType(), cmdNode.getFlags());
              cmd.setKeys(keys);
              currentCmd.push(cmd);
            }
            else {
              // Oops - this wasn't a motion command. The user goofed and typed something else
              mode = STATE_BAD_COMMAND;
            }
          }
          else if (currentArg == Argument.EX_STRING && (cmdNode.getFlags() & Command.FLAG_COMPLETE_EX) != 0) {
            String text = CommandGroups.getInstance().getProcess().endSearchCommand(editor, context);
            Argument arg = new Argument(text);
            Command cmd = currentCmd.peek();
            cmd.setArgument(arg);
            CommandState.getInstance(editor).popState();
          }
          // The user entered a valid command that doesn't take any arguments
          else {
            // Create the command and add it to the stack
            Command cmd = new Command(count, cmdNode.getActionId(), cmdNode.getAction(),
                                      cmdNode.getCmdType(), cmdNode.getFlags());
            cmd.setKeys(keys);
            currentCmd.push(cmd);

            // This is a sanity check that the command has a valid action. This should only fail if the
            // programmer made a typo or forgot to add the action to the plugin.xml file
            if (cmd.getAction() == null) {
              logger.error("NULL action for keys '" + keys + "'");
              mode = STATE_ERROR;
            }
          }
        }
        // If this is an argument node then the last keystroke was not part of the current command but should
        // be the first keystroke of the current command's argument
        else if (node instanceof ArgumentNode) {
          logger.debug("argument node");
          // Create a new command based on what the user has typed so far, excluding this keystroke.
          ArgumentNode arg = (ArgumentNode)node;
          Command cmd = new Command(count, arg.getActionId(), arg.getAction(), arg.getCmdType(), arg.getFlags());
          cmd.setKeys(keys);
          currentCmd.push(cmd);
          // What type of argument does this command expect?
          switch (arg.getArgType()) {
            case Argument.DIGRAPH:
              //digraphState = 0;
              digraph = new DigraphSequence();
              // No break - fall through
            case Argument.CHARACTER:
            case Argument.MOTION:
              mode = STATE_NEW_COMMAND;
              currentArg = arg.getArgType();
              // Is the current command an operator? If so set the state to only accept "operator pending"
              // commands
              if ((arg.getFlags() & Command.FLAG_OP_PEND) != 0) {
                //CommandState.getInstance().setMappingMode(KeyParser.MAPPING_OP_PEND);
                editorState.pushState(editorState.getMode(), editorState.getSubMode(),
                                      KeyParser.MAPPING_OP_PEND);
              }
              break;
            case Argument.EX_STRING:
              /*
              mode = STATE_NEW_COMMAND;
              currentArg = arg.getArgType();
              editorState.pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
              */
              break;
            default:
              // Oops - we aren't expecting any other type of argument
              mode = STATE_ERROR;
          }

          // If the current keystroke is really the first character of an argument the user needs to enter,
          // recursively go back and handle this keystroke again with all the state properly updated to
          // handle the argument
          if (currentArg != Argument.NONE) {
            partialReset(editor);
            boolean saveRecording = isRecording;
            handleKey(editor, key, context);
            isRecording = saveRecording;
            shouldRecord = false; // Prevent this from getting recorded twice
          }
        }
        else {
          logger.debug("checking for digraph");
          logger.debug("lastWasBS=" + lastWasBS);
          logger.debug("lastChar=" + lastChar);
          if (lastWasBS && lastChar != 0 && Options.getInstance().isSet("digraph")) {
            char dig = CommandGroups.getInstance().getDigraph().getDigraph(lastChar, key.getKeyChar());
            logger.debug("dig=" + dig);
            key = KeyStroke.getKeyStroke(dig);
          }

          // If we are in insert/replace mode send this key in for processing
          if (editorState.getMode() == CommandState.MODE_INSERT ||
              editorState.getMode() == CommandState.MODE_REPLACE) {
            if (!CommandGroups.getInstance().getChange().processKey(editor, context, key)) {
              shouldRecord = false;
            }
          }
          else if (editorState.getMappingMode() == KeyParser.MAPPING_CMD_LINE) {
            if (!CommandGroups.getInstance().getProcess().processExKey(editor, context, key, true)) {
              shouldRecord = false;
            }
          }
          // If we get here then the user has entered an unrecognized series of keystrokes
          else {
            mode = STATE_BAD_COMMAND;
          }

          lastChar = key.getKeyChar();
          partialReset(editor);
        }
      }
      break;
    }

    // Do we have a fully entered command at this point? If so, lets execute it
    if (mode == STATE_READY) {
      DelegateCommandListener.getInstance().setRunnable(null);
      // Let's go through the command stack and merge it all into one command. At this time there should never
      // be more than two commands on the stack - one is the actual command and the other would be a motion
      // command argument needed by the first command
      Command cmd = currentCmd.pop();
      while (currentCmd.size() > 0) {
        Command top = currentCmd.pop();
        top.setArgument(new Argument(cmd));
        cmd = top;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("cmd=" + cmd);
      }
      // If we have a command and a motion command argument, both could possibly have their own counts. We
      // need to adjust the counts so the motion gets the product of both counts and the command's count gets
      // reset. Example 3c2w (change 2 words, three times) becomes c6w (change 6 words)
      Argument arg = cmd.getArgument();
      if (logger.isDebugEnabled()) {
        logger.debug("arg=" + arg);
      }
      if (arg != null && arg.getType() == Argument.MOTION) {
        Command mot = arg.getMotion();
        // If no count was entered for either command then nothing changes. If either had a count then
        // the motion gets the product of both.
        int cnt = cmd.getRawCount() == 0 && mot.getRawCount() == 0 ? 0 : cmd.getCount() * mot.getCount();
        cmd.setCount(0);
        mot.setCount(cnt);
      }

      // If we were in "operator pending" mode, reset back to normal mode.
      if (editorState.getMappingMode() == KeyParser.MAPPING_OP_PEND) {
        //CommandState.getInstance().setMappingMode(KeyParser.MAPPING_NORMAL);
        editorState.popState();
      }

      // Save off the command we are about to execute
      editorState.setCommand(cmd);

      lastWasBS = ((cmd.getFlags() & Command.FLAG_IS_BACKSPACE) != 0);
      logger.debug("lastWasBS=" + lastWasBS);

      Project project = editor.getProject();
      if (cmd.isReadType() || EditorHelper.canEdit(project, editor)) {
        Runnable action = new ActionRunner(editor, context, cmd, key);
        if (cmd.isWriteType()) {
          RunnableHelper.runWriteCommand(project, action, cmd.getActionId(), null);
        }
        else {
          RunnableHelper.runReadCommand(project, action, cmd.getActionId(), null);
        }
      }
      else {
        logger.info("write command on read-only file");
        VimPlugin.indicateError();
        reset(editor);
      }
    }
    else if (mode == STATE_BAD_COMMAND) {
      VimPlugin.indicateError();
      reset(editor);
    }
    // We had some sort of error so reset the handler and let the user know (beep)
    else if (mode == STATE_ERROR) {
      VimPlugin.indicateError();
      fullReset(editor);
    }
    else if (isRecording && shouldRecord) {
      CommandGroups.getInstance().getRegister().addKeyStroke(key);
    }
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  public static void executeAction(String name, DataContext context) {
    logger.debug("executing action " + name);
    ActionManager aMgr = ActionManager.getInstance();
    AnAction action = aMgr.getAction(name);
    if (action != null) {
      executeAction(action, context);
    }
    else {
      logger.debug("Unknown action");
    }
  }

  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  public static void executeAction(AnAction action, DataContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug("executing action " + action);
    }

    // Hopefully all the arguments are sufficient. So far they all seem to work OK.
    // We don't have a specific InputEvent so that is null
    // What is "place"? Leave it the empty string for now.
    // Is the template presentation sufficient?
    // What are the modifiers? Is zero OK?
    action.actionPerformed(new AnActionEvent(
      null,
      context,
      "",
      action.getTemplatePresentation(),
      ActionManager.getInstance(), // API change - don't merge
      0));
  }

  /**
   * Partially resets the state of this handler. Resets the command count, clears the key list, resets the key tree
   * node to the root for the current mode we are in.
   *
   * @param editor The editor to reset.
   */
  private void partialReset(Editor editor) {
    count = 0;
    keys = new ArrayList<KeyStroke>();
    CommandState editorState = CommandState.getInstance(editor);
    editorState.setCurrentNode(KeyParser.getInstance().getKeyRoot(editorState.getMappingMode()));
    logger.debug("partialReset");
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument
   *
   * @param editor The editor to reset.
   */
  public void reset(Editor editor) {
    partialReset(editor);
    mode = STATE_NEW_COMMAND;
    currentCmd.clear();
    currentArg = Argument.NONE;
    digraph = null;
    logger.debug("reset");
  }

  /**
   * Completely resets the state of this handler. Resets the command mode to normal, resets, and clears the selected
   * register.
   *
   * @param editor The editor to reset.
   */
  public void fullReset(Editor editor) {
    CommandState.getInstance(editor).reset();
    reset(editor);
    lastChar = 0;
    lastWasBS = false;
    CommandGroups.getInstance().getRegister().resetRegister();
    DelegateCommandListener.getInstance().setRunnable(null);
  }

  /**
   * This was used as an experiment to execute actions as a runnable.
   */
  static class ActionRunner implements Runnable {
    public ActionRunner(Editor editor, DataContext context, Command cmd, KeyStroke key) {
      this.editor = editor;
      this.context = context;
      this.cmd = cmd;
      this.key = key;
    }

    public void run() {
      CommandState editorState = CommandState.getInstance(editor);
      boolean wasRecording = editorState.isRecording();

      executeAction(cmd.getAction(), context);
      if (editorState.getMode() == CommandState.MODE_INSERT ||
          editorState.getMode() == CommandState.MODE_REPLACE) {
        CommandGroups.getInstance().getChange().processCommand(editor, context, cmd);
      }

      // Now that the command has been executed let's clean up a few things.

      // By default the "empty" register is used by all commands so we want to reset whatever the last register
      // selected by the user was to the empty register - unless we just executed the "select register" command.
      if (cmd.getType() != Command.SELECT_REGISTER) {
        CommandGroups.getInstance().getRegister().resetRegister();
      }

      // If, at this point, we are not in insert, replace, or visual modes, we need to restore the previous
      // mode we were in. This handles commands in those modes that temporarily allow us to execute normal
      // mode commands. An exception is if this command should leave us in the temporary mode such as
      // "select register"
      if (editorState.getSubMode() == CommandState.SUBMODE_SINGLE_COMMAND &&
          (cmd.getFlags() & Command.FLAG_EXPECT_MORE) == 0) {
        editorState.popState();
      }

      KeyHandler.getInstance().reset(editor);

      if (wasRecording && editorState.isRecording()) {
        CommandGroups.getInstance().getRegister().addKeyStroke(key);
      }
    }

    private Editor editor;
    private DataContext context;
    private Command cmd;
    private KeyStroke key;
  }

  private int count;
  private List<KeyStroke> keys;
  private int mode;
  private Stack<Command> currentCmd = new Stack<Command>();
  private int currentArg;
  private TypedActionHandler origHandler;
  private DigraphSequence digraph = null;
  private char lastChar;
  private boolean lastWasBS;

  private static KeyHandler instance;

  private static final int STATE_NEW_COMMAND = 1;
  private static final int STATE_COMMAND = 2;
  private static final int STATE_READY = 3;
  private static final int STATE_ERROR = 4;
  private static final int STATE_BAD_COMMAND = 5;

  private static Logger logger = Logger.getInstance(KeyHandler.class.getName());
}
