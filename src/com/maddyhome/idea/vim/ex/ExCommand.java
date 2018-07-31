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

package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ExCommand {
  public ExCommand(@NotNull Ranges ranges, @NotNull String command, @NotNull String argument) {
    this.ranges = ranges;
    this.argument = argument;
    this.command = command;
  }

  public int getLine(@NotNull Editor editor, DataContext context) {
    return ranges.getLine(editor, context);
  }

  public int getLine(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context) {
    return ranges.getLine(editor, caret, context);
  }

  public List<Integer> getOrderedLines(@NotNull Editor editor, @NotNull DataContext context,
                                       @NotNull CaretOrder caretOrder) {
    final ArrayList<Integer> lines = new ArrayList<>(editor.getCaretModel().getCaretCount());
    for (Caret caret : EditorHelper.getOrderedCaretsList(editor, caretOrder)) {
      final int line = getLine(editor, caret, context);
      lines.add(line);
    }
    return lines;
  }

  public int getCount(@NotNull Editor editor, DataContext context, int defaultCount, boolean checkCount) {
    int count = -1;
    if (checkCount) {
      count = getCountArgument();
    }

    int res = ranges.getCount(editor, context, count);
    if (res == -1) {
      res = defaultCount;
    }

    return res;
  }

  public int getCount(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int defaultCount,
                      boolean checkCount) {
    final int count = ranges.getCount(editor, caret, context, checkCount ? getCountArgument() : -1);
    if (count == -1) return defaultCount;
    return count;
  }

  @NotNull
  public LineRange getLineRange(@NotNull Editor editor, DataContext context) {
    return ranges.getLineRange(editor, context, -1);
  }

  public LineRange getLineRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context) {
    return ranges.getLineRange(editor, caret, context, -1);
  }

  @NotNull
  public TextRange getTextRange(@NotNull Editor editor, DataContext context, boolean checkCount) {
    int count = -1;
    if (checkCount) {
      count = getCountArgument();
    }

    return ranges.getTextRange(editor, context, count);
  }

  public TextRange getTextRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                boolean checkCount) {
    return ranges.getTextRange(editor, caret, context, checkCount ? getCountArgument() : -1);
  }

  private int getCountArgument() {
    try {
      return Integer.parseInt(argument);
    }
    catch (NumberFormatException e) {
      return -1;
    }
  }

  @NotNull
  public String getCommand() {
    return command;
  }

  @NotNull
  public String getArgument() {
    return argument;
  }

  public void setArgument(@NotNull String argument) {
    this.argument = argument;
  }

  @NotNull
  public Ranges getRanges() {
    return ranges;
  }

  @NotNull
  private final Ranges ranges;
  @NotNull
  private final String command;
  @NotNull
  private String argument;
}
