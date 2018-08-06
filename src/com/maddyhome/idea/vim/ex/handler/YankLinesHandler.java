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
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.ArrayUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.RegisterGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

/**
 *
 */
public class YankLinesHandler extends CommandHandler {
  public YankLinesHandler() {
    super("y", "ank", RANGE_OPTIONAL | ARGUMENT_OPTIONAL);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final String argument = cmd.getArgument();
    final RegisterGroup registerGroup = VimPlugin.getRegister();
    final char register;
    if (argument.length() > 0 && !Character.isDigit(argument.charAt(0))) {
      register = argument.charAt(0);
      cmd.setArgument(argument.substring(1));
    }
    else {
      register = registerGroup.getDefaultRegister();
    }

    if (!registerGroup.selectRegister(register)) return false;

    final CaretModel caretModel = editor.getCaretModel();
    final List<Integer> starts = newArrayListWithCapacity(caretModel.getCaretCount());
    final List<Integer> ends = newArrayListWithCapacity(caretModel.getCaretCount());
    for (Caret caret : caretModel.getAllCarets()) {
      final TextRange range = cmd.getTextRange(editor, caret, context, true);
      starts.add(range.getStartOffset());
      ends.add(range.getEndOffset() - 1);
    }

    return VimPlugin.getCopy().yankRange(editor,
                                         new TextRange(ArrayUtil.toIntArray(starts), ArrayUtil.toIntArray(ends)),
                                         SelectionType.LINE_WISE, false);
  }
}
