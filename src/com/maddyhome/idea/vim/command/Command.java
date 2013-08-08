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

package com.maddyhome.idea.vim.command;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * This represents a single Vim command to be executed. It may optionally include an argument if appropriate for
 * the command. The command has a count and a type.
 */
public class Command {
  /**
   * Motion flags
   */
  public static final int FLAG_MOT_LINEWISE = 1 << 1;
  public static final int FLAG_MOT_CHARACTERWISE = 1 << 2;
  public static final int FLAG_MOT_BLOCKWISE = 1 << 3;
  public static final int FLAG_MOT_INCLUSIVE = 1 << 4;
  public static final int FLAG_MOT_EXCLUSIVE = 1 << 5;
  /**
   * Indicates that the cursor position should be saved prior to this motion command
   */
  public static final int FLAG_SAVE_JUMP = 1 << 6;
  /**
   * Special flag that says this is characterwise only for visual mode
   */
  public static final int FLAG_VISUAL_CHARACTERWISE = 1 << 7;

  /**
   * Special command flag that indicates it is not to be repeated
   */
  public static final int FLAG_NO_REPEAT = 1 << 8;
  /**
   * This insert command should clear all saved keystrokes from the current insert
   */
  public static final int FLAG_CLEAR_STROKES = 1 << 9;
  /**
   * This keystroke should be saved as part of the current insert
   */
  public static final int FLAG_SAVE_STROKE = 1 << 10;
  /**
   * This is a backspace command
   */
  public static final int FLAG_IS_BACKSPACE = 1 << 11;

  public static final int FLAG_IGNORE_SCROLL_JUMP = 1 << 12;
  public static final int FLAG_IGNORE_SIDE_SCROLL_JUMP = 1 << 13;

  /**
   * Indicates a command can accept a count in mid command
   */
  public static final int FLAG_ALLOW_MID_COUNT = 1 << 14;

  /**
   * Search Flags
   */
  public static final int FLAG_SEARCH_FWD = 1 << 16;
  public static final int FLAG_SEARCH_REV = 1 << 17;

  public static final int FLAG_KEEP_VISUAL = 1 << 20;
  public static final int FLAG_FORCE_VISUAL = 1 << 21;
  public static final int FLAG_FORCE_LINEWISE = 1 << 22;
  public static final int FLAG_DELEGATE = 1 << 23;
  /**
   * Special flag used for any mappings involving operators
   */
  public static final int FLAG_OP_PEND = 1 << 24;
  /**
   * This command starts a multi-command undo transaction
   */
  public static final int FLAG_MULTIKEY_UNDO = 1 << 25;
  /**
   * This command should be followed by another command
   */
  public static final int FLAG_EXPECT_MORE = 1 << 26;
  /**
   * This flag indicates the command's argument isn't used while recording
   */
  public static final int FLAG_NO_ARG_RECORDING = 1 << 27;
  /**
   * Indicate that the character argument may come from a digraph
   */
  public static final int FLAG_ALLOW_DIGRAPH = 1 << 28;
  public static final int FLAG_COMPLETE_EX = 1 << 29;
  public static final int FLAG_TEXT_BLOCK = 1 << 30;

  public static enum Type {
    /**
     * Represents undefined commands.
     */
    UNDEFINED,
    /**
     * Represents commands that actually move the cursor and can be arguments to operators.
     */
    MOTION,
    /**
     * Represents commands that insert new text into the editor.
     */
    INSERT,
    /**
     * Represents commands that remove text from the editor.
     */
    DELETE,
    /**
     * Represents commands that change text in the editor.
     */
    CHANGE,
    /**
     * Represents commands that copy text in the editor.
     */
    COPY,
    PASTE,
    RESET,
    /**
     * Represents commands that select the register.
     */
    SELECT_REGISTER,
    OTHER_READONLY,
    OTHER_WRITABLE,
    OTHER_READ_WRITE,
    COMPLETION;

    public boolean isRead() {
      switch (this) {
        case MOTION:
        case COPY:
        case SELECT_REGISTER:
        case OTHER_READONLY:
        case OTHER_READ_WRITE:
        case COMPLETION:
          return true;
        default:
          return false;
      }
    }

    public boolean isWrite() {
      switch (this) {
        case INSERT:
        case DELETE:
        case CHANGE:
        case PASTE:
        case RESET:
        case OTHER_WRITABLE:
        case OTHER_READ_WRITE:
          return true;
        default:
          return false;
      }
    }
  }

  /**
   * Creates a command that doesn't require an argument
   *
   * @param count  The number entered prior to the command (zero if no specific number)
   * @param action The action to be executed when the command is run
   * @param type   The type of the command
   * @param flags  Any custom flags specific to this command
   */
  public Command(int count, String actionId, AnAction action, @NotNull Type type, int flags) {
    this(count, actionId, action, type, flags, null);
  }

  /**
   * Creates a command that requires an argument
   *
   * @param count  The number entered prior to the command (zero if no specific number)
   * @param action The action to be executed when the command is run
   * @param type   The type of the command
   * @param flags  Any custom flags specific to this command
   * @param arg    The argument to this command
   */
  public Command(int count, String actionId, AnAction action, @NotNull Type type, int flags, Argument arg) {
    this.count = count;
    this.actionId = actionId;
    this.action = action;
    this.type = type;
    this.flags = flags;
    this.argument = arg;

    if (action instanceof EditorAction) {
      EditorAction eaction = (EditorAction)action;
      EditorActionHandler handler = eaction.getHandler();
      if (handler instanceof AbstractEditorActionHandler) {
        ((AbstractEditorActionHandler)handler).process(this);
      }
    }
  }

  /**
   * Returns the command count. A zero count is returned as one since that is the default for most commands
   *
   * @return The command count
   */
  public int getCount() {
    return count == 0 ? 1 : count;
  }

  /**
   * Updates the command count to the new value
   *
   * @param count The new command count
   */
  public void setCount(int count) {
    this.count = count;
  }

  /**
   * Gets to actual count entered by the user, including zero if no count was specified. Some commands need to
   * know whether an actual count was specified or not.
   *
   * @return The actual count entered by the user
   */
  public int getRawCount() {
    return count;
  }

  /**
   * Gets the command type
   *
   * @return The command type
   */
  @NotNull
  public Type getType() {
    return type;
  }

  /**
   * Gets the flags associated with the command
   *
   * @return The command flags
   */
  public int getFlags() {
    return flags;
  }

  /**
   * Sets new flags for the command
   *
   * @param flags The new flags
   */
  public void setFlags(int flags) {
    this.flags = flags;
  }

  public String getActionId() {
    return actionId;
  }

  public void setActionId(String actionId) {
    this.actionId = actionId;
  }

  /**
   * Gets the action to execute when the command is run
   *
   * @return The command's action
   */
  public AnAction getAction() {
    return action;
  }

  /**
   * Sets a new action for the command
   *
   * @param action The new action
   */
  public void setAction(AnAction action) {
    this.action = action;
  }

  /**
   * Gets the command's argument, if any.
   *
   * @return The command's argument, null if there isn't one
   */
  public Argument getArgument() {
    return argument;
  }

  /**
   * Sets the command's argument to the new value
   *
   * @param argument The new argument, can be null to clear the argument
   */
  public void setArgument(Argument argument) {
    this.argument = argument;
  }

  public List<KeyStroke> getKeys() {
    return keys;
  }

  public void setKeys(List<KeyStroke> keys) {
    this.keys = keys;
  }

  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("Command[");
    res.append("count=").append(count);
    res.append(", actionId=").append(actionId);
    res.append(", action=").append(action);
    res.append(", type=").append(type);
    res.append(", flags=").append(flags);
    res.append(", argument=").append(argument);
    res.append(", keys=").append(keys);
    res.append("]");

    return res.toString();
  }

  private int count;
  private String actionId;
  private AnAction action;
  @NotNull private Type type;
  private int flags;
  private Argument argument;
  private List<KeyStroke> keys;
}
