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

package com.maddyhome.idea.vim.action.change.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import org.jetbrains.annotations.NotNull;

/**
 */
public class FilterMotionAction extends EditorAction {
  public FilterMotionAction() {
    super(new Handler());
  }

  private static class Handler extends EditorActionHandlerBase {
    protected boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
      final Argument argument = cmd.getArgument();
      if (argument == null) {
        return false;
      }
      TextRange range = MotionGroup
        .getMotionRange(editor, editor.getCaretModel().getPrimaryCaret(), context, cmd.getCount(), cmd.getRawCount(),
                        argument, false);
      if (range == null) {
        return false;
      }

      LogicalPosition current = editor.getCaretModel().getLogicalPosition();
      LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
      LogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
      if (current.line != start.line) {
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), range.getStartOffset());
      }

      int count;
      if (start.line < end.line) {
        count = end.line - start.line + 1;
      }
      else {
        count = 1;
      }

      Command command = new Command(count, null, null, Command.Type.UNDEFINED, 0);
      VimPlugin.getProcess().startFilterCommand(editor, context, command);

      return true;
    }
  }
}
