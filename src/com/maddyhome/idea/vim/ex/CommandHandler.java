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

package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.handler.ExecuteMethodNotOverriddenException;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Base class for all Ex command handlers.
 */
public abstract class CommandHandler {
  public enum Flag {
    /**
     * Indicates that a range must be specified with this command
     */
    RANGE_REQUIRED,
    /**
     * Indicates that a range is optional for this command
     */
    RANGE_OPTIONAL,
    /**
     * Indicates that a range can't be specified for this command
     */
    RANGE_FORBIDDEN,
    /**
     * Indicates that an argument must be specified with this command
     */
    ARGUMENT_REQUIRED,
    /**
     * Indicates that an argument is optional for this command
     */
    ARGUMENT_OPTIONAL,
    /**
     * Indicates that an argument can't be specified for this command
     */
    ARGUMENT_FORBIDDEN,
    /**
     * Indicates that the command takes a count, not a range - effects default
     */
    RANGE_IS_COUNT,

    DONT_REOPEN,

    /**
     * Indicates that this is a command that modifies the editor
     */
    WRITABLE,
    /**
     * Indicates that this command does not modify the editor
     */
    READ_ONLY,
    DONT_SAVE_LAST,
  }
  /**
   * Create the handler
   *
   * @param names A list of names this command answers to
   * @param flags Range and Arguments commands
   */
  public CommandHandler(CommandName[] names, EnumSet<Flag> flags) {
    this(names, flags, EnumSet.noneOf(CommandFlags.class), false, CaretOrder.NATIVE);
  }

  public CommandHandler(CommandName[] names, EnumSet<Flag> flags, boolean runForEachCaret, CaretOrder caretOrder) {
    this(names, flags, EnumSet.noneOf(CommandFlags.class), runForEachCaret, caretOrder);
  }

  public CommandHandler(@Nullable CommandName[] names, EnumSet<Flag> argFlags, EnumSet<CommandFlags> optFlags, boolean runForEachCaret, CaretOrder caretOrder) {
    this.names = names;
    this.argFlags = argFlags;
    this.optFlags = optFlags;

    myRunForEachCaret = runForEachCaret;
    myCaretOrder = caretOrder;

    CommandParser.getInstance().addHandler(this);
  }

  public CommandHandler(EnumSet<Flag> argFlags, EnumSet<CommandFlags> optFlags, boolean runForEachCaret, CaretOrder caretOrder) {
    this.names = null;
    this.argFlags = argFlags;
    this.optFlags = optFlags;

    myRunForEachCaret = runForEachCaret;
    myCaretOrder = caretOrder;
  }

  /**
   * Gets all the command names
   *
   * @return The command names
   */
  @Nullable
  public CommandName[] getNames() {
    return names;
  }

  /**
   * Gets the range and argument flags
   *
   * @return The range and argument flags
   */
  public EnumSet<Flag> getArgFlags() {
    return argFlags;
  }

  /**
   * Executes a command. The range and arugments are validated first.
   *
   * @param editor  The editor to run the command in
   * @param context The data context
   * @param cmd     The command as entered by the user
   * @param count   The count entered by the user prior to the command
   * @throws ExException if the range or argument is invalid or unable to run the command
   */
  public boolean process(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd, int count) throws ExException {
    // No range allowed
    if (argFlags.contains(Flag.RANGE_FORBIDDEN) && cmd.getRanges().size() != 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_norange));
      throw new NoRangeAllowedException();
    }

    if (argFlags.contains(Flag.RANGE_REQUIRED) && cmd.getRanges().size() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_rangereq));
      throw new MissingRangeException();
    }

    // Argument required
    if (argFlags.contains(Flag.ARGUMENT_REQUIRED) && cmd.getArgument().length() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argreq));
      throw new MissingArgumentException();
    }

    if (argFlags.contains(Flag.RANGE_IS_COUNT)) {
      cmd.getRanges().setDefaultLine(1);
    }

    CommandState.getInstance(editor).setFlags(optFlags);

    boolean res = true;
    try {
      if (myRunForEachCaret) {
        final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, myCaretOrder);
        for (Caret caret : carets) {
          for (int i = 0; i < count && res; i++) {
            try {
              res = execute(editor, caret, context, cmd);
            } catch (ExecuteMethodNotOverriddenException e) {
              return false;
            }
          }
        }
      }
      else {
        for (int i = 0; i < count && res; i++) {
          try {
            res = execute(editor, context, cmd);
          } catch (ExecuteMethodNotOverriddenException e) {
            return false;
          }
        }
      }

      if (!res) {
        VimPlugin.indicateError();
      }
      return res;
    }
    catch (ExException e) {
      VimPlugin.showMessage(e.getMessage());
      VimPlugin.indicateError();
      return false;
    }
  }

  /**
   * Performs the action of the handler.
   *
   * @param editor  The editor to perform the action in.
   * @param context The data context
   * @param cmd     The complete Ex command including range, command, and arguments
   * @return True if able to perform the command, false if not
   * @throws ExException if the range or arguments are invalid for the command
   */
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException, ExecuteMethodNotOverriddenException {
    if (!myRunForEachCaret) throw new ExecuteMethodNotOverriddenException(this.getClass());
    return execute(editor, editor.getCaretModel().getPrimaryCaret(), context, cmd);
  }

  public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                          @NotNull ExCommand cmd) throws ExException, ExecuteMethodNotOverriddenException {
    if (myRunForEachCaret) throw new ExecuteMethodNotOverriddenException(this.getClass());
    return execute(editor, context, cmd);
  }

  @Nullable private final CommandName[] names;
  private final EnumSet<Flag> argFlags;
  private final EnumSet<CommandFlags> optFlags;

  private final boolean myRunForEachCaret;
  private final CaretOrder myCaretOrder;
}
