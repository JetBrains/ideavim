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
import com.intellij.util.ArrayUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.handler.CaretOrder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ShiftLeftHandler extends CommandHandler {
  public ShiftLeftHandler() {
    super("<", "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<", ARGUMENT_OPTIONAL | WRITABLE, true, CaretOrder.DECREASING_OFFSET);
  }

  public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, @NotNull ExCommand cmd) {
    final TextRange range = cmd.getTextRange(editor, caret, context, true);
    final int[] endOffsets = range.getEndOffsets();
    final List<Integer> ends = new ArrayList<>();
    for (int endOffset : endOffsets) {
      ends.add(endOffset - 1);
    }
    VimPlugin.getChange().indentRange(editor, caret, context,
                                      new TextRange(range.getStartOffsets(), ArrayUtil.toIntArray(ends)),
                                      cmd.getCommand().length(), -1);
    return true;
  }
}
