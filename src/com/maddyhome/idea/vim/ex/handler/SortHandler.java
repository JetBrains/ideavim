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
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.LineRange;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * @author Alex Selesse
 */
public class SortHandler extends CommandHandler {
  public SortHandler() {
    super("sor", "t", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final String arg = cmd.getArgument();
    final boolean nonEmptyArg = arg.trim().length() > 0;

    final boolean reverse = nonEmptyArg && arg.contains("!");
    final boolean ignoreCase = nonEmptyArg && arg.contains("i");
    final boolean number = nonEmptyArg && arg.contains("n");

    final Comparator<String> lineComparator = new LineComparator(ignoreCase, number, reverse);
    if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_BLOCK) {
      final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
      final LineRange range = getLineRange(editor, primaryCaret, context, cmd);
      final boolean worked = VimPlugin.getChange().sortRange(editor, range, lineComparator);
      primaryCaret.moveToOffset(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, range.getStartLine()));
      return worked;
    }

    boolean worked = true;
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      final LineRange range = getLineRange(editor, caret, context, cmd);
      if (!VimPlugin.getChange().sortRange(editor, range, lineComparator)) {
        worked = false;
      }
      caret.moveToOffset(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, range.getStartLine()));
    }

    return worked;
  }

  @NotNull
  private LineRange getLineRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                 @NotNull ExCommand cmd) {
    final LineRange range = cmd.getLineRange(editor, caret, context);
    final LineRange normalizedRange;

    // Something like "30,20sort" gets converted to "20,30sort"
    if (range.getEndLine() < range.getStartLine()) {
      normalizedRange = new LineRange(range.getEndLine(), range.getStartLine());
    }
    else {
      normalizedRange = range;
    }

    // If we don't have a range, we either have "sort", a selection, or a block
    if (normalizedRange.getEndLine() - normalizedRange.getStartLine() == 0) {
      // If we have a selection.
      final SelectionModel selectionModel = editor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        final int start = selectionModel.getSelectionStart();
        final int end = selectionModel.getSelectionEnd();

        final int startLine = editor.offsetToLogicalPosition(start).line;
        final int endLine = editor.offsetToLogicalPosition(end).line;

        return new LineRange(startLine, endLine);
      }
      // If we have a generic selection, i.e. "sort" entire document
      else {
        return new LineRange(0, editor.getDocument().getLineCount() - 1);
      }
    }

    return normalizedRange;
  }

  private static class LineComparator implements Comparator<String> {
    private final boolean myIgnoreCase;
    private final boolean myNumber;
    private final boolean myReverse;

    public LineComparator(boolean ignoreCase, boolean number, boolean reverse) {
      myIgnoreCase = ignoreCase;
      myNumber = number;
      myReverse = reverse;
    }

    @Override
    public int compare(String o1, String o2) {
      if (myReverse) {
        final String tmp = o2;
        o2 = o1;
        o1 = tmp;
      }
      if (myIgnoreCase) {
        o1 = o1.toUpperCase();
        o2 = o2.toUpperCase();
      }
      return myNumber ? StringUtil.naturalCompare(o1, o2) : o1.compareTo(o2);
    }
  }
}
