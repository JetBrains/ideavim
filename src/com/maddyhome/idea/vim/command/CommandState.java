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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.key.ParentNode;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;

public class CommandState {
  public static final int DEFAULT_TIMEOUT_LENGTH = 1000;

  @Nullable private static Command ourLastChange = null;
  private char myLastChangeRegister;

  @NotNull private final Stack<State> myStates = new Stack<State>();
  @NotNull private final State myDefaultState = new State(Mode.COMMAND, SubMode.NONE, MappingMode.NORMAL);
  @Nullable private Command myCommand;
  @NotNull private ParentNode myCurrentNode = VimPlugin.getKey().getKeyRoot(getMappingMode());
  @NotNull private final List<KeyStroke> myMappingKeys = new ArrayList<KeyStroke>();
  @NotNull private final Timer myMappingTimer;
  private EnumSet<CommandFlags> myFlags = EnumSet.noneOf(CommandFlags.class);
  private boolean myIsRecording = false;

  private CommandState() {
    myMappingTimer = new Timer(DEFAULT_TIMEOUT_LENGTH, null);
    myMappingTimer.setRepeats(false);
    myStates.push(new State(Mode.COMMAND, SubMode.NONE, MappingMode.NORMAL));
    myLastChangeRegister = VimPlugin.getRegister().getDefaultRegister();
  }

  @NotNull
  public static CommandState getInstance(@Nullable Editor editor) {
    if (editor == null) {
      return new CommandState();
    }

    CommandState res = EditorData.getCommandState(editor);
    if (res == null) {
      res = new CommandState();
      EditorData.setCommandState(editor, res);
    }

    return res;
  }

  public static boolean inInsertMode(@Nullable Editor editor) {
    final Mode mode = getInstance(editor).getMode();
    return mode == Mode.INSERT || mode == Mode.REPLACE;
  }

  public static boolean inRepeatMode(@Nullable Editor editor) {
    final Mode mode = getInstance(editor).getMode();
    return mode == Mode.REPEAT;
  }

  public static boolean inVisualCharacterMode(@Nullable Editor editor) {
    final CommandState state = getInstance(editor);
    return state.getMode() == Mode.VISUAL && state.getSubMode() == SubMode.VISUAL_CHARACTER;
  }

  public static boolean inVisualMode(@Nullable Editor editor) {
    return getInstance(editor).getMode() == Mode.VISUAL;
  }

  public static boolean inVisualLineMode(@Nullable Editor editor) {
    final CommandState state = getInstance(editor);
    return state.getMode() == Mode.VISUAL && state.getSubMode() == SubMode.VISUAL_LINE;
  }

  public static boolean inVisualBlockMode(@Nullable Editor editor) {
    final CommandState state = getInstance(editor);
    return state.getMode() == Mode.VISUAL && state.getSubMode() == SubMode.VISUAL_BLOCK;
  }

  public static boolean inSingleCommandMode(@Nullable Editor editor) {
    final CommandState state = getInstance(editor);
    return state.getMode() == Mode.COMMAND && state.getSubMode() == SubMode.SINGLE_COMMAND;
  }

  @Nullable
  public Command getCommand() {
    return myCommand;
  }

  public void setCommand(@NotNull Command cmd) {
    myCommand = cmd;
    setFlags(cmd.getFlags());
  }

  public EnumSet<CommandFlags> getFlags() {
    return myFlags;
  }

  public void setFlags(EnumSet<CommandFlags> flags) {
    this.myFlags = flags;
  }

  public void pushState(@NotNull Mode mode, @NotNull SubMode submode, @NotNull MappingMode mappingMode) {
    myStates.push(new State(mode, submode, mappingMode));
    updateStatus();
  }

  public void popState() {
    myStates.pop();
    updateStatus();
  }

  @NotNull
  public Mode getMode() {
    return currentState().getMode();
  }

  @NotNull
  public SubMode getSubMode() {
    return currentState().getSubMode();
  }

  public void setSubMode(@NotNull SubMode submode) {
    currentState().setSubMode(submode);
    updateStatus();
  }

  @NotNull
  public List<KeyStroke> getMappingKeys() {
    return myMappingKeys;
  }

  public void startMappingTimer(@NotNull ActionListener actionListener) {
    final NumberOption timeoutLength = Options.getInstance().getNumberOption("timeoutlen");
    if (timeoutLength != null) {
      myMappingTimer.setInitialDelay(timeoutLength.value());
    }
    for (ActionListener listener : myMappingTimer.getActionListeners()) {
      myMappingTimer.removeActionListener(listener);
    }
    myMappingTimer.addActionListener(actionListener);
    myMappingTimer.start();
  }

  public void stopMappingTimer() {
    myMappingTimer.stop();
  }

  @NotNull
  private String getStatusString(int pos) {
    State state;
    if (pos >= 0 && pos < myStates.size()) {
      state = myStates.get(pos);
    }
    else if (pos < 0) {
      state = myDefaultState;
    }
    else {
      return "";
    }

    final StringBuilder msg = new StringBuilder();
    switch (state.getMode()) {
      case COMMAND:
        if (state.getSubMode() == SubMode.SINGLE_COMMAND) {
          msg.append('(').append(getStatusString(pos - 1).toLowerCase()).append(')');
        }
        break;
      case INSERT:
        msg.append("INSERT");
        break;
      case REPLACE:
        msg.append("REPLACE");
        break;
      case VISUAL:
        if (pos > 0) {
          State tmp = myStates.get(pos - 1);
          if (tmp.getMode() == Mode.COMMAND && tmp.getSubMode() == SubMode.SINGLE_COMMAND) {
            msg.append(getStatusString(pos - 1));
            msg.append(" - ");
          }
        }
        switch (state.getSubMode()) {
          case VISUAL_LINE:
            msg.append("VISUAL LINE");
            break;
          case VISUAL_BLOCK:
            msg.append("VISUAL BLOCK");
            break;
          default:
            msg.append("VISUAL");
        }
        break;
    }

    return msg.toString();
  }

  /**
   * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
   * mode.
   */
  public void toggleInsertOverwrite() {
    Mode oldMode = getMode();
    Mode newMode = oldMode;
    if (oldMode == Mode.INSERT) {
      newMode = Mode.REPLACE;
    }
    else if (oldMode == Mode.REPLACE) {
      newMode = Mode.INSERT;
    }

    if (oldMode != newMode) {
      State state = currentState();
      popState();
      pushState(newMode, state.getSubMode(), state.getMappingMode());
    }
  }

  /**
   * Resets the command, mode, visual mode, and mapping mode to initial values.
   */
  public void reset() {
    myCommand = null;
    myStates.clear();
    updateStatus();
  }

  /**
   * Gets the current key mapping mode
   *
   * @return The current key mapping mode
   */
  @NotNull
  public MappingMode getMappingMode() {
    return currentState().getMappingMode();
  }

  /**
   * Gets the last command that performed a change
   *
   * @return The last change command, null if there hasn't been a change yet
   */
  @Nullable
  public Command getLastChangeCommand() {
    return ourLastChange;
  }

  /**
   * Gets the register used by the last saved change command
   *
   * @return The register key
   */
  public char getLastChangeRegister() {
    return myLastChangeRegister;
  }

  /**
   * Saves the last command that performed a change. It also preserves the register the command worked with.
   *
   * @param cmd The change command
   */
  public void saveLastChangeCommand(Command cmd) {
    ourLastChange = cmd;
    myLastChangeRegister = VimPlugin.getRegister().getCurrentRegister();
  }

  public boolean isRecording() {
    return myIsRecording;
  }

  public void setRecording(boolean val) {
    myIsRecording = val;
    updateStatus();
  }

  @NotNull
  public ParentNode getCurrentNode() {
    return myCurrentNode;
  }

  public void setCurrentNode(@NotNull ParentNode currentNode) {
    this.myCurrentNode = currentNode;
  }

  private State currentState() {
    if (myStates.size() > 0) {
      return myStates.peek();
    }
    else {
      return myDefaultState;
    }
  }

  private void updateStatus() {
    final StringBuilder msg = new StringBuilder();
    if (Options.getInstance().isSet("showmode")) {
      msg.append(getStatusString(myStates.size() - 1));
    }

    if (isRecording()) {
      if (msg.length() > 0) {
        msg.append(" - ");
      }
      msg.append("recording");
    }

    VimPlugin.showMode(msg.toString());
  }

  public enum Mode {
    COMMAND,
    INSERT,
    REPLACE,
    REPEAT,
    VISUAL,
    EX_ENTRY
  }

  public enum SubMode {
    NONE,
    SINGLE_COMMAND,
    VISUAL_CHARACTER,
    VISUAL_LINE,
    VISUAL_BLOCK
  }

  private class State {
    @NotNull private final Mode myMode;
    @NotNull private SubMode mySubMode;
    @NotNull private final MappingMode myMappingMode;

    public State(@NotNull Mode mode, @NotNull SubMode subMode, @NotNull MappingMode mappingMode) {
      this.myMode = mode;
      this.mySubMode = subMode;
      this.myMappingMode = mappingMode;
    }

    @NotNull
    public Mode getMode() {
      return myMode;
    }

    @NotNull
    public SubMode getSubMode() {
      return mySubMode;
    }

    public void setSubMode(@NotNull SubMode subMode) {
      this.mySubMode = subMode;
    }

    @NotNull
    public MappingMode getMappingMode() {
      return myMappingMode;
    }
  }
}
