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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.CaretData;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class JoinLinesHandler extends CommandHandler {
  public JoinLinesHandler() {
    super("j", "oin", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE, true, CaretOrder.DECREASING_OFFSET);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final StringBuilder arg = new StringBuilder(cmd.getArgument());
    final boolean spaces;
    if (arg.length() > 0 && arg.charAt(0) == '!') {
      spaces = false;
      arg.deleteCharAt(0);
    }
    else spaces = true;

    final TextRange textRange =
        CommandState.getInstance(editor).getMode() != CommandState.Mode.VISUAL ? cmd.getTextRange(editor, caret,
                                                                                                  context, true)
                                                                               : CaretData.getVisualTextRange(caret);
    if (textRange == null) return false;

    return VimPlugin.getChange().deleteJoinRange(editor, caret, new TextRange(textRange.getStartOffset(),
                                                                              textRange.getEndOffset() - 1), spaces);
  }
}
