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

package com.maddyhome.idea.vim;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  @NotNull
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
   * @return The original key handler
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
  public void handleKey(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull DataContext context) {
    VimPlugin.clearError();
    // All the editor actions should be performed with top level editor!!!
    // Be careful: all the EditorActionHandler implementation should correctly process InjectedEditors
    editor = InjectedLanguageUtil.getTopLevelEditor(editor);
    final CommandState editorState = CommandState.getInstance(editor);
    final boolean isRecording = editorState.isRecording();
    boolean shouldRecord = true;
    // If this is a "regular" character keystroke, get the character
    char chKey = key.getKeyChar() == KeyEvent.CHAR_UNDEFINED ? 0 : key.getKeyChar();

    if (isEditorReset(key, editorState)) {
      handleEditorReset(editor, key, context);
    }
    // At this point the user must be typing in a command. Most commands can be preceded by a number. Let's
    // check if a number can be entered at this point, and if so, did the user send us a digit.
    else if (isCommandCount(editorState, chKey)) {
      // Update the count
      count = count * 10 + (chKey - '0');
    }
    // Pressing delete while entering a count "removes" the last digit entered
    else if (isDeleteCommandCount(key, editorState)) {
      // "Remove" the last digit sent to us
      count /= 10;
    }
    // If we got this far the user is entering a command or supplying an argument to an entered command.
    // First let's check to see if we are at the point of expecting a single character argument to a command.
    else if (currentArg == Argument.Type.CHARACTER) {
      handleCharArgument(key, chKey);
    }
    // If we are this far, then the user must be entering a command or a non-single-character argument
    // to an entered command. Let's figure out which it is
    else {
      // For debugging purposes we track the keys entered for this command
      keys.add(key);

      // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
      // return the node matching this keystroke
      final Node node = editorState.getCurrentNode().getChild(key);

      if (handleDigraph(editor, key, context, node)) {
        return;
      }

      // If this is a branch node we have entered only part of a multi-key command
      if (node instanceof BranchNode) {
        handleBranchNode(editor, context, editorState, chKey, (BranchNode)node);
      }
      // If this is a command node the user has entered a valid key sequence of a known command
      else if (node instanceof CommandNode) {
        handleCommandNode(editor, context, (CommandNode)node);
      }
      // If this is an argument node then the last keystroke was not part of the current command but should
      // be the first keystroke of the argument of the current command
      else if (node instanceof ArgumentNode) {
        shouldRecord = handleArgumentNode(editor, key, context, editorState, true, (ArgumentNode)node);
      }
      else {
        if (lastWasBS && lastChar != 0 && Options.getInstance().isSet("digraph")) {
          char dig = CommandGroups.getInstance().getDigraph().getDigraph(lastChar, key.getKeyChar());
          key = KeyStroke.getKeyStroke(dig);
        }

        // If we are in insert/replace mode send this key in for processing
        if (editorState.getMode() == CommandState.Mode.INSERT || editorState.getMode() == CommandState.Mode.REPLACE) {
          if (!CommandGroups.getInstance().getChange().processKey(editor, context, key)) {
            shouldRecord = false;
          }
        }
        else if (editorState.getMappingMode() == KeyParser.MAPPING_CMD_LINE) {
          if (!CommandGroups.getInstance().getProcess().processExKey(editor, key, true)) {
            shouldRecord = false;
          }
        }
        // If we get here then the user has entered an unrecognized series of keystrokes
        else {
          state = State.BAD_COMMAND;
        }

        lastChar = key.getKeyChar();
        partialReset(editor);
      }
    }

    // Do we have a fully entered command at this point? If so, lets execute it
    if (state == State.READY) {
      executeCommand(editor, key, context, editorState);
    }
    else if (state == State.BAD_COMMAND) {
      if (editorState.getMappingMode() == KeyParser.MAPPING_OP_PEND) {
        editorState.popState();
      }
      else {
        VimPlugin.indicateError();
        reset(editor);
      }
    }
    // We had some sort of error so reset the handler and let the user know (beep)
    else if (state == State.ERROR) {
      VimPlugin.indicateError();
      fullReset(editor);
    }
    else if (isRecording && shouldRecord) {
      CommandGroups.getInstance().getRegister().recordKeyStroke(key);
    }
  }

  private void handleEditorReset(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull final DataContext context) {
    if (state != State.COMMAND && count == 0 && currentArg == Argument.Type.NONE && currentCmd.size() == 0 &&
        CommandGroups.getInstance().getRegister().getCurrentRegister() == RegisterGroup.REGISTER_DEFAULT) {
      if (key.getKeyCode() == KeyEvent.VK_ESCAPE) {
        CommandProcessor.getInstance().executeCommand(editor.getProject(), new Runnable() {
          @Override
          public void run() {
            KeyHandler.executeAction("VimEditorEscape", context);
          }
        }, "", null);
      }
      VimPlugin.indicateError();
    }
    reset(editor);
  }

  private boolean isDeleteCommandCount(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    return (editorState.getMode() == CommandState.Mode.COMMAND || editorState.getMode() == CommandState.Mode.VISUAL) &&
           state == State.NEW_COMMAND && currentArg != Argument.Type.CHARACTER && currentArg != Argument.Type.DIGRAPH &&
           key.getKeyCode() == KeyEvent.VK_DELETE && count != 0;
  }

  private boolean isCommandCount(@NotNull CommandState editorState, char chKey) {
    return (editorState.getMode() == CommandState.Mode.COMMAND || editorState.getMode() == CommandState.Mode.VISUAL) &&
           state == State.NEW_COMMAND && currentArg != Argument.Type.CHARACTER && currentArg != Argument.Type.DIGRAPH &&
           Character.isDigit(chKey) &&
           (count != 0 || chKey != '0');
  }

  private boolean isEditorReset(@NotNull KeyStroke key, @NotNull CommandState editorState) {
    return (editorState.getMode() == CommandState.Mode.COMMAND || state == State.COMMAND) &&
           (key.getKeyCode() == KeyEvent.VK_ESCAPE ||
            (key.getKeyCode() == KeyEvent.VK_C && (key.getModifiers() & KeyEvent.CTRL_MASK) != 0) ||
            (key.getKeyCode() == '[' && (key.getModifiers() & KeyEvent.CTRL_MASK) != 0));
  }

  private void handleCharArgument(@NotNull KeyStroke key, char chKey) {
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
      state = State.READY;
    }
    else {
      // Oops - this isn't a valid character argument
      state = State.BAD_COMMAND;
    }
  }

  private boolean handleDigraph(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull DataContext context,
                                @Nullable Node node) {
    if (digraph == null && !(node instanceof CommandNode) && DigraphSequence.isDigraphStart(key)) {
      digraph = new DigraphSequence();
    }
    if (digraph != null) {
      DigraphSequence.DigraphResult res = digraph.processKey(key, editor);
      switch (res.getResult()) {
        case DigraphSequence.DigraphResult.RES_OK:
          return true;
        case DigraphSequence.DigraphResult.RES_BAD:
          digraph = null;
          return true;
        case DigraphSequence.DigraphResult.RES_DONE:
          if (currentArg == Argument.Type.DIGRAPH) {
            currentArg = Argument.Type.CHARACTER;
          }
          digraph = null;
          final KeyStroke stroke = res.getStroke();
          if (stroke == null) {
            return false;
          }
          handleKey(editor, stroke, context);
          return true;
      }
    }
    return false;
  }

  private void executeCommand(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull DataContext context,
                              @NotNull CommandState editorState) {
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

    // If we have a command and a motion command argument, both could possibly have their own counts. We
    // need to adjust the counts so the motion gets the product of both counts and the count associated with
    // the command gets reset. Example 3c2w (change 2 words, three times) becomes c6w (change 6 words)
    final Argument arg = cmd.getArgument();
    if (arg != null && arg.getType() == Argument.Type.MOTION) {
      final Command mot = arg.getMotion();
      // If no count was entered for either command then nothing changes. If either had a count then
      // the motion gets the product of both.
      if (mot != null) {
        int cnt = cmd.getRawCount() == 0 && mot.getRawCount() == 0 ? 0 : cmd.getCount() * mot.getCount();
        mot.setCount(cnt);
      }
      cmd.setCount(0);
    }

    // If we were in "operator pending" mode, reset back to normal mode.
    if (editorState.getMappingMode() == KeyParser.MAPPING_OP_PEND) {
      //CommandState.getInstance().setMappingMode(KeyParser.MAPPING_NORMAL);
      editorState.popState();
    }

    // Save off the command we are about to execute
    editorState.setCommand(cmd);

    lastWasBS = ((cmd.getFlags() & Command.FLAG_IS_BACKSPACE) != 0);

    Project project = editor.getProject();
    if (cmd.getType().isRead() || project == null || EditorHelper.canEdit(project, editor)) {
      if (ApplicationManager.getApplication().isDispatchThread()) {
        Runnable action = new ActionRunner(editor, context, cmd, key);
        String name = cmd.getAction().getTemplatePresentation().getText();
        name = name != null ? "Vim " + name : "";
        if (cmd.getType().isWrite()) {
          RunnableHelper.runWriteCommand(project, action, name, action);
        }
        else {
          RunnableHelper.runReadCommand(project, action, name, action);
        }
      }
    }
    else {
      VimPlugin.indicateError();
      reset(editor);
    }
  }

  private boolean handleArgumentNode(@NotNull Editor editor, @NotNull KeyStroke key, @NotNull DataContext context,
                                     @NotNull CommandState editorState, boolean shouldRecord,
                                     @NotNull ArgumentNode node) {
    // Create a new command based on what the user has typed so far, excluding this keystroke.
    Command cmd = new Command(count, node.getActionId(), node.getAction(), node.getCmdType(), node.getFlags());
    cmd.setKeys(keys);
    currentCmd.push(cmd);
    // What type of argument does this command expect?
    switch (node.getArgType()) {
      case DIGRAPH:
        //digraphState = 0;
        digraph = new DigraphSequence();
        // No break - fall through
      case CHARACTER:
      case MOTION:
        state = State.NEW_COMMAND;
        currentArg = node.getArgType();
        // Is the current command an operator? If so set the state to only accept "operator pending"
        // commands
        if ((node.getFlags() & Command.FLAG_OP_PEND) != 0) {
          //CommandState.getInstance().setMappingMode(KeyParser.MAPPING_OP_PEND);
          editorState.pushState(editorState.getMode(), editorState.getSubMode(), KeyParser.MAPPING_OP_PEND);
        }
        break;
      case EX_STRING:
        break;
      default:
        // Oops - we aren't expecting any other type of argument
        state = State.ERROR;
    }

    // If the current keystroke is really the first character of an argument the user needs to enter,
    // recursively go back and handle this keystroke again with all the state properly updated to
    // handle the argument
    if (currentArg != Argument.Type.NONE) {
      partialReset(editor);
      handleKey(editor, key, context);
      shouldRecord = false; // Prevent this from getting recorded twice
    }
    return shouldRecord;
  }

  private void handleCommandNode(@NotNull Editor editor, @NotNull DataContext context, @NotNull CommandNode node) {
    // If all does well we are ready to process this command
    state = State.READY;
    // Did we just get the completed sequence for a motion command argument?
    if (currentArg == Argument.Type.MOTION) {
      // We have been expecting a motion argument - is this one?
      if (node.getCmdType() == Command.Type.MOTION) {
        // Create the motion command and add it to the stack
        Command cmd = new Command(count, node.getActionId(), node.getAction(), node.getCmdType(), node.getFlags());
        cmd.setKeys(keys);
        currentCmd.push(cmd);
      }
      else if (node.getCmdType() == Command.Type.RESET) {
        currentCmd.clear();
        Command cmd = new Command(1, node.getActionId(), node.getAction(), node.getCmdType(), node.getFlags());
        cmd.setKeys(keys);
        currentCmd.push(cmd);
      }
      else {
        // Oops - this wasn't a motion command. The user goofed and typed something else
        state = State.BAD_COMMAND;
      }
    }
    else if (currentArg == Argument.Type.EX_STRING && (node.getFlags() & Command.FLAG_COMPLETE_EX) != 0) {
      String text = CommandGroups.getInstance().getProcess().endSearchCommand(editor, context);
      Argument arg = new Argument(text);
      Command cmd = currentCmd.peek();
      cmd.setArgument(arg);
      CommandState.getInstance(editor).popState();
    }
    // The user entered a valid command that doesn't take any arguments
    else {
      // Create the command and add it to the stack
      Command cmd = new Command(count, node.getActionId(), node.getAction(), node.getCmdType(), node.getFlags());
      cmd.setKeys(keys);
      currentCmd.push(cmd);

      // This is a sanity check that the command has a valid action. This should only fail if the
      // programmer made a typo or forgot to add the action to the plugin.xml file
      if (cmd.getAction() == null) {
        state = State.ERROR;
      }
    }
  }

  private void handleBranchNode(@NotNull Editor editor, @NotNull DataContext context, @NotNull CommandState editorState,
                                char key, @NotNull BranchNode node) {
    // Flag that we aren't allowing any more count digits (unless it's OK)
    if ((node.getFlags() & Command.FLAG_ALLOW_MID_COUNT) == 0) {
      state = State.COMMAND;
    }
    editorState.setCurrentNode(node);

    ArgumentNode arg = (ArgumentNode)((BranchNode)editorState.getCurrentNode()).getArgumentNode();
    if (arg != null) {
      if (currentArg == Argument.Type.MOTION && arg.getCmdType() != Command.Type.MOTION) {
        editorState.popState();
        state = State.BAD_COMMAND;
        return;
      }
      if (editorState.isRecording() && (arg.getFlags() & Command.FLAG_NO_ARG_RECORDING) != 0) {
        handleKey(editor, KeyStroke.getKeyStroke(' '), context);
      }

      if (arg.getArgType() == Argument.Type.EX_STRING) {
        CommandGroups.getInstance().getProcess().startSearchCommand(editor, context, count, key);
        state = State.NEW_COMMAND;
        currentArg = Argument.Type.EX_STRING;
        editorState.pushState(CommandState.Mode.EX_ENTRY, CommandState.SubMode.NONE, KeyParser.MAPPING_CMD_LINE);
      }
    }
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  public static void executeAction(@NotNull String name, @NotNull DataContext context) {
    ActionManager aMgr = ActionManager.getInstance();
    AnAction action = aMgr.getAction(name);
    if (action != null) {
      executeAction(action, context);
    }
  }

  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  public static void executeAction(@NotNull AnAction action, @NotNull DataContext context) {
    // Hopefully all the arguments are sufficient. So far they all seem to work OK.
    // We don't have a specific InputEvent so that is null
    // What is "place"? Leave it the empty string for now.
    // Is the template presentation sufficient?
    // What are the modifiers? Is zero OK?
    action.actionPerformed(
      new AnActionEvent(null, context, "", action.getTemplatePresentation(), ActionManager.getInstance(),
                        // API change - don't merge
                        0));
  }

  /**
   * Partially resets the state of this handler. Resets the command count, clears the key list, resets the key tree
   * node to the root for the current mode we are in.
   *
   * @param editor The editor to reset.
   */
  private void partialReset(@Nullable Editor editor) {
    count = 0;
    keys = new ArrayList<KeyStroke>();
    CommandState editorState = CommandState.getInstance(editor);
    editorState.setCurrentNode(KeyParser.getInstance().getKeyRoot(editorState.getMappingMode()));
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument
   *
   * @param editor The editor to reset.
   */
  public void reset(@Nullable Editor editor) {
    partialReset(editor);
    state = State.NEW_COMMAND;
    currentCmd.clear();
    currentArg = Argument.Type.NONE;
    digraph = null;
  }

  /**
   * Completely resets the state of this handler. Resets the command mode to normal, resets, and clears the selected
   * register.
   *
   * @param editor The editor to reset.
   */
  public void fullReset(@Nullable Editor editor) {
    VimPlugin.clearError();
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
      if (editorState.getMode() == CommandState.Mode.INSERT || editorState.getMode() == CommandState.Mode.REPLACE) {
        CommandGroups.getInstance().getChange().processCommand(editor, cmd);
      }

      // Now that the command has been executed let's clean up a few things.

      // By default the "empty" register is used by all commands so we want to reset whatever the last register
      // selected by the user was to the empty register - unless we just executed the "select register" command.
      if (cmd.getType() != Command.Type.SELECT_REGISTER) {
        CommandGroups.getInstance().getRegister().resetRegister();
      }

      // If, at this point, we are not in insert, replace, or visual modes, we need to restore the previous
      // mode we were in. This handles commands in those modes that temporarily allow us to execute normal
      // mode commands. An exception is if this command should leave us in the temporary mode such as
      // "select register"
      if (editorState.getSubMode() == CommandState.SubMode.SINGLE_COMMAND &&
          (cmd.getFlags() & Command.FLAG_EXPECT_MORE) == 0) {
        editorState.popState();
      }

      KeyHandler.getInstance().reset(editor);

      if (wasRecording && editorState.isRecording()) {
        CommandGroups.getInstance().getRegister().recordKeyStroke(key);
      }
    }

    private Editor editor;
    private DataContext context;
    private Command cmd;
    private KeyStroke key;
  }

  private static enum State {
    NEW_COMMAND,
    COMMAND,
    READY,
    ERROR,
    BAD_COMMAND
  }

  private int count;
  private List<KeyStroke> keys;
  private State state;
  @NotNull private Stack<Command> currentCmd = new Stack<Command>();
  @NotNull private Argument.Type currentArg;
  private TypedActionHandler origHandler;
  @Nullable private DigraphSequence digraph = null;
  private char lastChar;
  private boolean lastWasBS;

  private static KeyHandler instance;
}
