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
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.RegisterGroup;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class DeleteLinesHandler extends CommandHandler {
  public DeleteLinesHandler() {
    super("d", "elete", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  public boolean execute(@NotNull Editor editor, DataContext context, @NotNull ExCommand cmd) throws ExException {
    StringBuffer arg = new StringBuffer(cmd.getArgument());
    char register = RegisterGroup.REGISTER_DEFAULT;
    if (arg.length() > 0 && (arg.charAt(0) < '0' || arg.charAt(0) > '9')) {
      register = arg.charAt(0);
      arg.deleteCharAt(0);
      cmd.setArgument(arg.toString());
    }

    CommandGroups.getInstance().getRegister().selectRegister(register);

    TextRange range = cmd.getTextRange(editor, context, true);

    return CommandGroups.getInstance().getChange().deleteRange(editor, range, SelectionType.LINE_WISE, false);
  }
}
