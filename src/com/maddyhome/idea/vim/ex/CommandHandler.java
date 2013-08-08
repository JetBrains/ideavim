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

package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Ex command handlers.
 */
public abstract class CommandHandler {
  /**
   * Indicates that a range must be specified with this command
   */
  public static final int RANGE_REQUIRED = 1;
  /**
   * Indicates that a range is optional for this command
   */
  public static final int RANGE_OPTIONAL = 2;
  /**
   * Indicates that a range can't be specified for this command
   */
  public static final int RANGE_FORBIDDEN = 4;
  /**
   * Indicates that an argument must be specified with this command
   */
  public static final int ARGUMENT_REQUIRED = 8;
  /**
   * Indicates that an argument is optional for this command
   */
  public static final int ARGUMENT_OPTIONAL = 16;
  /**
   * Indicates that an argument can't be specified for this command
   */
  public static final int ARGUMENT_FORBIDDEN = 32;
  /**
   * Indicates that the command takes a count, not a range - effects default
   */
  public static final int RANGE_IS_COUNT = 64;
  /**
   * Indicates that the editor should not get focus back after the command
   */
  public static final int KEEP_FOCUS = 128;
  public static final int DONT_REOPEN = 256;

  /**
   * Indicates that this is a command that modifies the editor
   */
  public static final int WRITABLE = 512;
  /**
   * Indicates that this command does not modify the editor
   */
  public static final int READ_ONLY = 1024;
  public static final int DONT_SAVE_LAST = 2048;

  /**
   * Create the handler
   *
   * @param names A list of names this command answers to
   * @param flags Range and Arguments commands
   */
  public CommandHandler(CommandName[] names, int flags) {
    this(names, flags, 0);
  }

  /**
   * Create the handler
   *
   * @param names    A list of names this command answers to
   * @param argFlags Range and Arguments commands
   * @param optFlags Other command specific flags
   */
  public CommandHandler(CommandName[] names, int argFlags, int optFlags) {
    this.names = names;
    this.argFlags = argFlags;
    this.optFlags = optFlags;

    CommandParser.getInstance().addHandler(this);
  }

  /**
   * Create the handler
   *
   * @param text     The required portion of the command name
   * @param optional The optional portion of the command name
   * @param argFlags Range and Arguments commands
   */
  public CommandHandler(String text, String optional, int argFlags) {
    this(text, optional, argFlags, 0);
  }

  /**
   * Create the handler
   *
   * @param text     The required portion of the command name
   * @param optional The optional portion of the command name
   * @param argFlags Range and Arguments commands
   * @param optFlags Other command specific flags
   */
  public CommandHandler(String text, String optional, int argFlags, int optFlags) {
    this(new CommandName[]{new CommandName(text, optional)}, argFlags, optFlags);
  }

  /**
   * Create the handler. Do not register the handler with the parser
   *
   * @param argFlags Range and Arguments commands
   */
  public CommandHandler(int argFlags) {
    this(argFlags, 0);
  }

  /**
   * Create the handler. Do not register the handler with the parser
   *
   * @param argFlags Range and Arguments commands
   * @param optFlags Other command specific flags
   */
  public CommandHandler(int argFlags, int optFlags) {
    this.names = null;
    this.argFlags = argFlags;
    this.optFlags = optFlags;
  }

  /**
   * Gets the required portion of the command name
   *
   * @return The required portion of the command name. Returns the first if there are several names
   */
  @Nullable
  public String getRequired() {
    if (names == null) {
      return null;
    }
    else {
      return names[0].getRequired();
    }
  }

  /**
   * Gets the optional portion of the command name
   *
   * @return The optional portion of the command name. Returns the first if there are several names
   */
  @Nullable
  public String getOptional() {
    if (names == null) {
      return null;
    }
    else {
      return names[0].getOptional();
    }
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
  public int getArgFlags() {
    return argFlags;
  }

  /**
   * Gets the command specific flags
   *
   * @return The command flags
   */
  public int getOptFlags() {
    return optFlags;
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
  public boolean process(final Editor editor, final DataContext context, @NotNull final ExCommand cmd, final int count) throws
                                                                                                               ExException {
    // No range allowed
    if ((argFlags & RANGE_FORBIDDEN) != 0 && cmd.getRanges().size() != 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_norange));
      throw new NoRangeAllowedException();
    }

    if ((argFlags & RANGE_REQUIRED) != 0 && cmd.getRanges().size() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_rangereq));
      throw new MissingRangeException();
    }

    // Argument required
    if ((argFlags & ARGUMENT_REQUIRED) != 0 && cmd.getArgument().length() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argreq));
      throw new MissingArgumentException();
    }

    if ((argFlags & RANGE_IS_COUNT) != 0) {
      cmd.getRanges().setDefaultLine(1);
    }

    CommandState.getInstance(editor).setFlags(optFlags);

    boolean res = true;
    if ((argFlags & WRITABLE) != 0) {
      try {
        for (int i = 0; i < count && res; i++) {
          res = execute(editor, context, cmd);
        }
      }
      catch (ExException e) {
        res = false;
      }
      finally {
        if (!res) {
          VimPlugin.indicateError();
        }
      }
    }
    else {
      try {
        for (int i = 0; i < count; i++) {
          res = execute(editor, context, cmd);
        }
        if (!res) {
          VimPlugin.indicateError();
        }
      }
      catch (ExException e) {
        VimPlugin.indicateError();
      }
    }

    return res;
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
  public abstract boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException;

  @Nullable protected CommandName[] names;
  protected int argFlags;
  protected int optFlags;
}
