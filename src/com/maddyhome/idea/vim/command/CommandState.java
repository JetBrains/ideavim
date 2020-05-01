/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
import com.maddyhome.idea.vim.helper.DigraphResult;
import com.maddyhome.idea.vim.helper.DigraphSequence;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.key.CommandPartNode;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.EnumSet;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Used to maintain state while entering a Vim command (operator, motion, text object, etc.)
 */
public class CommandState {
  private static final Logger logger = Logger.getInstance(CommandState.class.getName());
  private static final ModeState defaultModeState = new ModeState(Mode.COMMAND, SubMode.NONE);

  private final CommandBuilder commandBuilder = new CommandBuilder(getKeyRootNode(MappingMode.NORMAL));
  private final Stack<ModeState> modeStates = new Stack<>();
  private final MappingState mappingState = new MappingState();
  private final DigraphSequence digraphSequence = new DigraphSequence();
  private boolean isRecording = false;
  private boolean dotRepeatInProgress = false;

  /**
   * The currently executing command
   *
   * This is a complete command, e.g. operator + motion. Some actions/helpers require additional context from flags in
   * the command/argument. Ideally, we would pass the command through KeyHandler#executeVimAction and
   * EditorActionHandlerBase#execute, but we also need to know the command type in MarkGroup#updateMarkFromDelete,
   * which is called via a document change event.
   *
   * This field is reset after the command has been executed.
   */
  private @Nullable Command executingCommand;

  private CommandState() {
    pushModes(defaultModeState.getMode(), defaultModeState.getSubMode());
  }

  @Contract("null -> new")
  public static @NotNull CommandState getInstance(@Nullable Editor editor) {
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

  private static @NotNull CommandPartNode getKeyRootNode(MappingMode mappingMode) {
    return VimPlugin.getKey().getKeyRoot(mappingMode);
  }

  // Keep the compatibility with the IdeaVim-EasyMotion plugin before the stable release
  @ApiStatus.ScheduledForRemoval(inVersion = "0.58")
  @Deprecated
  public MappingMode getMappingMode() {
    return mappingState.getMappingMode();
  }

  public boolean isOperatorPending() {
    return mappingState.getMappingMode() == MappingMode.OP_PENDING && !commandBuilder.isEmpty();
  }

  public boolean isDuplicateOperatorKeyStroke(KeyStroke key) {
    return isOperatorPending() && commandBuilder.isDuplicateOperatorKeyStroke(key);
  }

  public CommandBuilder getCommandBuilder() {
    return commandBuilder;
  }

  public @NotNull MappingState getMappingState() {
    return mappingState;
  }

  public @Nullable Command getExecutingCommand() {
    return executingCommand;
  }

  public void setExecutingCommand(@NotNull Command cmd) {
    executingCommand = cmd;
  }

  public EnumSet<CommandFlags> getExecutingCommandFlags() {
    return executingCommand != null ? executingCommand.getFlags() : EnumSet.noneOf(CommandFlags.class);
  }

  public boolean isRecording() {
    return isRecording;
  }

  public void setRecording(boolean val) {
    isRecording = val;
    updateStatus();
  }

  public boolean isDotRepeatInProgress() {
    return dotRepeatInProgress;
  }

  public void setDotRepeatInProgress(boolean dotRepeatInProgress) {
    this.dotRepeatInProgress = dotRepeatInProgress;
  }

  public void pushModes(@NotNull Mode mode, @NotNull SubMode submode) {
    final ModeState newModeState = new ModeState(mode, submode);
    logger.info("Push new mode state: " + newModeState.toSimpleString());
    if (logger.isDebugEnabled()) {
      logger.debug("Stack of mode states before push: " + toSimpleString());
    }
    modeStates.push(newModeState);
    setMappingMode();
    updateStatus();
  }

  public void popModes() {
    final ModeState popped = modeStates.pop();
    setMappingMode();
    updateStatus();
    logger.info("Popped mode state: " + popped.toSimpleString());
    if (logger.isDebugEnabled()) {
      logger.debug("Stack of mode states after pop: " + toSimpleString());
    }
  }

  public void resetOpPending() {
    if (getSubMode() == SubMode.OP_PENDING) {
      popModes();
    }
  }

  public void resetRegisterPending() {
    if (getSubMode() == SubMode.REGISTER_PENDING) {
      popModes();
    }
  }

  private void resetModes() {
    modeStates.clear();
    setMappingMode();
  }

  private void setMappingMode() {
    final ModeState modeState = currentModeState();
    if (modeState.getSubMode() == SubMode.OP_PENDING) {
      mappingState.setMappingMode(MappingMode.OP_PENDING);
    }
    else {
      mappingState.setMappingMode(modeToMappingMode(getMode()));
    }
  }

  @Contract(pure = true)
  private MappingMode modeToMappingMode(@NotNull Mode mode) {
    switch (mode) {
      case COMMAND:
        return MappingMode.NORMAL;
      case INSERT:
      case REPLACE:
        return MappingMode.INSERT;
      case VISUAL:
        return MappingMode.VISUAL;
      case SELECT:
        return MappingMode.SELECT;
      case CMD_LINE:
        return MappingMode.CMD_LINE;
    }

    throw new IllegalArgumentException("Unexpected mode: " + mode);
  }

  public @NotNull Mode getMode() {
    return currentModeState().getMode();
  }

  public @NotNull SubMode getSubMode() {
    return currentModeState().getSubMode();
  }

  public void setSubMode(@NotNull SubMode submode) {
    final ModeState modeState = currentModeState();
    popModes();
    pushModes(modeState.getMode(), submode);
    updateStatus();
  }

  public void startDigraphSequence() {
    digraphSequence.startDigraphSequence();
  }

  public void startLiteralSequence() {
    digraphSequence.startLiteralSequence();
  }

  public DigraphResult processDigraphKey(KeyStroke key, Editor editor) {
    return digraphSequence.processKey(key, editor);
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
      ModeState modeState = currentModeState();
      popModes();
      pushModes(newMode, modeState.getSubMode());
    }
  }

  /**
   * Resets the command, mode, visual mode, and mapping mode to initial values.
   */
  public void reset() {
    executingCommand = null;
    resetModes();
    commandBuilder.resetInProgressCommandPart(getKeyRootNode(mappingState.getMappingMode()));
    startDigraphSequence();

    updateStatus();
  }

  public String toSimpleString() {
    return modeStates.stream().map(ModeState::toSimpleString)
      .collect(Collectors.joining(", "));
  }

  private ModeState currentModeState() {
    if (modeStates.size() > 0) {
      return modeStates.peek();
    }
    else {
      return defaultModeState;
    }
  }

  private void updateStatus() {
    final StringBuilder msg = new StringBuilder();
    if (OptionsManager.INSTANCE.getShowmode().isSet()) {
      msg.append(getStatusString(modeStates.size() - 1));
    }

    if (isRecording()) {
      if (msg.length() > 0) {
        msg.append(" - ");
      }
      msg.append("recording");
    }

    VimPlugin.showMode(msg.toString());
  }

  private @NotNull String getStatusString(int pos) {
    ModeState modeState;
    if (pos >= 0 && pos < modeStates.size()) {
      modeState = modeStates.get(pos);
    }
    else if (pos < 0) {
      modeState = defaultModeState;
    }
    else {
      return "";
    }

    final StringBuilder msg = new StringBuilder();
    switch (modeState.getMode()) {
      case COMMAND:
        if (modeState.getSubMode() == SubMode.SINGLE_COMMAND) {
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
          ModeState tmp = modeStates.get(pos - 1);
          if (tmp.getMode() == Mode.COMMAND && tmp.getSubMode() == SubMode.SINGLE_COMMAND) {
            msg.append(getStatusString(pos - 1));
            msg.append(" - ");
          }
        }
        switch (modeState.getSubMode()) {
          case VISUAL_LINE:
            msg.append(modeState.getMode()).append(" LINE");
            break;
          case VISUAL_BLOCK:
            msg.append(modeState.getMode()).append(" BLOCK");
            break;
          default:
            msg.append(modeState.getMode());
        }
        break;
    }

    return msg.toString();
  }

  public enum Mode {
    COMMAND, INSERT, REPLACE, VISUAL, SELECT, CMD_LINE
  }

  public enum SubMode {
    NONE, SINGLE_COMMAND, OP_PENDING, REGISTER_PENDING, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  private static class ModeState {
    private final @NotNull Mode myMode;
    private final @NotNull SubMode mySubMode;

    @Contract(pure = true)
    public ModeState(@NotNull Mode mode, @NotNull SubMode subMode) {
      this.myMode = mode;
      this.mySubMode = subMode;
    }

    public @NotNull Mode getMode() {
      return myMode;
    }

    public @NotNull SubMode getSubMode() {
      return mySubMode;
    }

    public String toSimpleString() {
      return myMode + ":" + mySubMode;
    }
  }
}
