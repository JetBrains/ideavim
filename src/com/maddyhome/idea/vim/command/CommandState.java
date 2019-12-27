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
import com.maddyhome.idea.vim.helper.DigraphResult;
import com.maddyhome.idea.vim.helper.DigraphSequence;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.key.CommandPartNode;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Used to maintain state while entering a Vim command (operator, motion, text object, etc.)
 */
public class CommandState {
  private static final Logger logger = Logger.getInstance(CommandState.class.getName());
  private static final ModeState defaultModeState = new ModeState(Mode.COMMAND, SubMode.NONE, MappingMode.NORMAL);

  @NotNull private final MappingState mappingState = new MappingState();
  @NotNull private final Stack<ModeState> modeStates = new Stack<>();
  private boolean isRecording = false;
  private boolean dotRepeatInProgress = false;

  /**
   * The last command executed
   */
  @Nullable private Command myCommand;
  private EnumSet<CommandFlags> myFlags = EnumSet.noneOf(CommandFlags.class);

  // State used to build the next command
  @NotNull private DigraphSequence digraphSequence = new DigraphSequence();
  @NotNull private final List<KeyStroke> keys = new ArrayList<>();
  @NotNull private CommandPartNode myCurrentNode = VimPlugin.getKey().getKeyRoot(getMappingMode());
  @Nullable private Argument.Type myCurrentArgumentType;
  private int count = 0;

  private CommandState() {
    modeStates.push(defaultModeState);
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

  @NotNull
  public MappingState getMappingState() {
    return mappingState;
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

  public void pushModes(@NotNull Mode mode, @NotNull SubMode submode, @NotNull MappingMode mappingMode) {
    final ModeState newModeState = new ModeState(mode, submode, mappingMode);
    logger.info("Push new mode state: " + newModeState.toSimpleString());
    if (logger.isDebugEnabled()) {
      logger.debug("Stack of mode states before push: " + toSimpleString());
    }
    modeStates.push(newModeState);
    updateStatus();
  }

  public void popModes() {
    final ModeState popped = modeStates.pop();
    updateStatus();
    logger.info("Popped mode state: " + popped.toSimpleString());
    if (logger.isDebugEnabled()) {
      logger.debug("Stack of mode states after pop: " + toSimpleString());
    }
  }

  @NotNull
  public Mode getMode() {
    return currentModeState().getMode();
  }

  @NotNull
  public SubMode getSubMode() {
    return currentModeState().getSubMode();
  }

  public void setSubMode(@NotNull SubMode submode) {
    final ModeState modeState = currentModeState();
    popModes();
    pushModes(modeState.getMode(), submode, modeState.getMappingMode());
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

  public int getCount() {
    return count;
  }

  public void setCount(int newCount) {
    count = newCount;
  }

  @NotNull
  public List<KeyStroke> getKeys() {
    return keys;
  }

  public void addKey(KeyStroke keyStroke) {
    keys.add(keyStroke);
  }

  @NotNull
  private String getStatusString(int pos) {
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
      pushModes(newMode, modeState.getSubMode(), modeState.getMappingMode());
    }
  }

  /**
   * Resets the command, mode, visual mode, and mapping mode to initial values.
   */
  public void reset() {
    myCommand = null;
    modeStates.clear();
    keys.clear();
    updateStatus();
    startDigraphSequence();
    count = 0;
  }

  /**
   * Gets the current key mapping mode
   *
   * @return The current key mapping mode
   */
  @NotNull
  public MappingMode getMappingMode() {
    return currentModeState().getMappingMode();
  }

  public boolean isRecording() {
    return isRecording;
  }

  public void setRecording(boolean val) {
    isRecording = val;
    updateStatus();
  }

  @NotNull
  public CommandPartNode getCurrentNode() {
    return myCurrentNode;
  }

  public void setCurrentNode(@NotNull CommandPartNode currentNode) {
    this.myCurrentNode = currentNode;
  }

  @Nullable
  public Argument.Type getCurrentArgumentType() {
    return myCurrentArgumentType;
  }

  public void setCurrentArgumentType(Argument.Type argumentType) {
    myCurrentArgumentType = argumentType;
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

  private static class ModeState {
    @NotNull private final Mode myMode;
    @NotNull private final SubMode mySubMode;
    @NotNull private final MappingMode myMappingMode;

    @Contract(pure = true)
    public ModeState(@NotNull Mode mode, @NotNull SubMode subMode, @NotNull MappingMode mappingMode) {
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

    @NotNull
    public MappingMode getMappingMode() {
      return myMappingMode;
    }

    public String toSimpleString() {
      return myMode + ":" + mySubMode;
    }
  }
}
