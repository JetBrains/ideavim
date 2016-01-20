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
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class YankLinesHandler extends CommandHandler {
  public YankLinesHandler() {
    super("y", "ank", RANGE_OPTIONAL | ARGUMENT_OPTIONAL);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException {
    StringBuilder arg = new StringBuilder(cmd.getArgument());
    char register = VimPlugin.getRegister().getDefaultRegister();
    if (arg.length() > 0 && (arg.charAt(0) < '0' || arg.charAt(0) > '9')) {
      register = arg.charAt(0);
      arg.deleteCharAt(0);
      cmd.setArgument(arg.toString());
    }

    if (!VimPlugin.getRegister().selectRegister(register)) {
      return false;
    }

    TextRange range = cmd.getTextRange(editor, context, true);

    return VimPlugin.getCopy().yankRange(editor, range, SelectionType.LINE_WISE, false);
  }
}
