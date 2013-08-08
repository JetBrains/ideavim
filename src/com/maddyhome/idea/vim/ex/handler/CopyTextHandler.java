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

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class CopyTextHandler extends CommandHandler {
  public CopyTextHandler() {
    super(new CommandName[]{
      new CommandName("co", "py"),
      new CommandName("t", "")
    }, RANGE_OPTIONAL | ARGUMENT_REQUIRED | WRITABLE);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException {
    TextRange range = cmd.getTextRange(editor, context, false);

    ParseResult pr = CommandParser.getInstance().parse(cmd.getArgument());
    int line = pr.getRanges().getFirstLine(editor, context);
    int offset = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor, line + 1);

    String text = EditorHelper.getText(editor, range.getStartOffset(), range.getEndOffset());
    CommandGroups.getInstance().getCopy().putText(editor, context, offset, text, SelectionType.LINE_WISE, 1, true,
                                                  false, CommandState.SubMode.NONE);

    return true;
  }
}
