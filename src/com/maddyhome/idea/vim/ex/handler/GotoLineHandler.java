/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

/**
 * This handles Ex commands that just specify a range which translates to moving the cursor to the line given by the
 * range.
 */
public class GotoLineHandler extends CommandHandler {
  /**
   * Create the handler
   */
  public GotoLineHandler() {
    super(RANGE_REQUIRED | ARGUMENT_OPTIONAL, Command.FLAG_MOT_EXCLUSIVE);
  }

  /**
   * Moves the cursor to the line entered by the user
   *
   * @param editor  The editor to perform the action in.
   * @param context The data context
   * @param cmd     The complete Ex command including range, command, and arguments
   * @return True if able to perform the command, false if not
   */
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) {
    // TODO: Add multiple carets support
    int count = cmd.getLine(editor, context);

    int max = EditorHelper.getLineCount(editor);
    if (count >= max) {
      count = max - 1;
    }

    if (count >= 0) {
      MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(),
                            VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, count));

      return true;
    }
    else {
      MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), 0);
    }

    return false;
  }
}
