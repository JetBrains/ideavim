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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ExCommand {
  public ExCommand(Ranges ranges, String command, String argument) {
    this.ranges = ranges;
    this.argument = argument;
    this.command = command;
  }

  public int getLine(@NotNull Editor editor, DataContext context) {
    return ranges.getLine(editor, context);
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

  @NotNull
  public LineRange getLineRange(@NotNull Editor editor, DataContext context, boolean checkCount) {
    int count = -1;
    if (checkCount) {
      count = getCountArgument();
    }

    return ranges.getLineRange(editor, context, count);
  }

  @NotNull
  public TextRange getTextRange(@NotNull Editor editor, DataContext context, boolean checkCount) {
    int count = -1;
    if (checkCount) {
      count = getCountArgument();
    }

    return ranges.getTextRange(editor, context, count);
  }

  protected int getCountArgument() {
    try {
      return Integer.parseInt(argument);
    }
    catch (NumberFormatException e) {
      return -1;
    }
  }

  public String getCommand() {
    return command;
  }

  public String getArgument() {
    return argument;
  }

  public void setArgument(String argument) {
    this.argument = argument;
  }

  public Ranges getRanges() {
    return ranges;
  }

  protected Ranges ranges;
  protected String command;
  protected String argument;

  private static Logger logger = Logger.getInstance(ExCommand.class.getName());
}
