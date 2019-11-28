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

package com.maddyhome.idea.vim.command;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.key.CommandPartNode;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class CommandState {
  private static final int DEFAULT_TIMEOUT_LENGTH = 1000;

  @NotNull private final Stack<State> myStates = new Stack<>();
  @NotNull private final State myDefaultState = new State(Mode.COMMAND, SubMode.NONE, MappingMode.NORMAL);
  @Nullable private Command myCommand;
  @NotNull private CommandPartNode myCurrentNode = VimPlugin.getKey().getKeyRoot(getMappingMode());
  @NotNull private final List<KeyStroke> myMappingKeys = new ArrayList<>();
  @NotNull private final Timer myMappingTimer;
  private EnumSet<CommandFlags> myFlags = EnumSet.noneOf(CommandFlags.class);
  private boolean myIsRecording = false;
  private static Logger logger = Logger.getInstance(CommandState.class.getName());
  private boolean dotRepeatInProgress = false;

  private CommandState() {
    myMappingTimer = new Timer(DEFAULT_TIMEOUT_LENGTH, null);
    myMappingTimer.setRepeats(false);
    myStates.push(new State(Mode.COMMAND, SubMode.NONE, MappingMode.NORMAL));
  }

  @Contract("null -> new")
  @NotNull
  public static CommandState getInstance(@Nullable Editor editor) {
    if (editor == null) {
      return new CommandState();
    }

    CommandState res = UserDataManager.getVimCommandState(editor);
    if (res == null) {
      res = new CommandState();
      UserDataManager.setVimCommandState(editor, res);
    }

    return res;
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
    final State newState = new State(mode, submode, mappingMode);
    logger.info("Push new state: " + newState.toSimpleString());
    if (logger.isDebugEnabled()) {
      logger.debug("Stack state before push: " + toSimpleString());
    }
    myStates.push(newState);
    updateStatus();
  }

  public void popState() {
    final State popped = myStates.pop();
    updateStatus();
    logger.info("Pop state: " + popped.toSimpleString());
    if (logger.isDebugEnabled()) {
      logger.debug("Stack state after pop: " + toSimpleString());
    }
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
    final NumberOption timeoutLength = OptionsManager.INSTANCE.getTimeoutlen();
    myMappingTimer.setInitialDelay(timeoutLength.value());
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
      case SELECT:
        if (pos > 0) {
          State tmp = myStates.get(pos - 1);
          if (tmp.getMode() == Mode.COMMAND && tmp.getSubMode() == SubMode.SINGLE_COMMAND) {
            msg.append(getStatusString(pos - 1));
            msg.append(" - ");
          }
        }
        switch (state.getSubMode()) {
          case VISUAL_LINE:
            msg.append(state.getMode()).append(" LINE");
            break;
          case VISUAL_BLOCK:
            msg.append(state.getMode()).append(" BLOCK");
            break;
          default:
            msg.append(state.getMode());
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

  public boolean isRecording() {
    return myIsRecording;
  }

  public void setRecording(boolean val) {
    myIsRecording = val;
    updateStatus();
  }

  @NotNull
  public CommandPartNode getCurrentNode() {
    return myCurrentNode;
  }

  public void setCurrentNode(@NotNull CommandPartNode currentNode) {
    this.myCurrentNode = currentNode;
  }

  public String toSimpleString() {
    return myStates.stream().map(State::toSimpleString)
      .collect(Collectors.joining(", "));
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
    if (OptionsManager.INSTANCE.getShowmode().isSet()) {
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

  public boolean isDotRepeatInProgress() {
    return dotRepeatInProgress;
  }

  public void setDotRepeatInProgress(boolean dotRepeatInProgress) {
    this.dotRepeatInProgress = dotRepeatInProgress;
  }

  public enum Mode {
    COMMAND, INSERT, REPLACE, VISUAL, SELECT, CMD_LINE
  }

  public enum SubMode {
    NONE, SINGLE_COMMAND, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  private static class State {
    @NotNull private final Mode myMode;
    @NotNull private SubMode mySubMode;
    @NotNull private final MappingMode myMappingMode;

    @Contract(pure = true)
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

    public String toSimpleString() {
      return myMode + ":" + mySubMode;
    }
  }
}
