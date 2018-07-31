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
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MoveTextHandler extends CommandHandler {
  public MoveTextHandler() {
    super("m", "ove", RANGE_OPTIONAL | ARGUMENT_REQUIRED | WRITABLE);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET);
    final CaretModel caretModel = editor.getCaretModel();
    final int caretCount = caretModel.getCaretCount();

    final List<String> texts = new ArrayList<>(caretCount);
    final List<TextRange> ranges = new ArrayList<>(caretCount);
    int line = EditorHelper.getFileSize(editor);
    final ExCommand command = CommandParser.getInstance().parse(cmd.getArgument());

    TextRange lastRange = null;
    for (Caret caret : carets) {
      final TextRange range = cmd.getTextRange(editor, caret, context, false);
      final LineRange lineRange = cmd.getLineRange(editor, caret, context);

      line = Math.min(line, normalizeLine(editor, caret, context, command, lineRange));
      texts.add(EditorHelper.getText(editor, range.getStartOffset(), range.getEndOffset()));

      if (lastRange == null ||
          (lastRange.getStartOffset() != range.getStartOffset() && lastRange.getEndOffset() != range.getEndOffset())) {
        ranges.add(range);
        lastRange = range;
      }
    }

    for (TextRange range : ranges) {
      editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
    }

    for (int i = 0; i < caretCount; i++) {
      final Caret caret = carets.get(i);
      final String text = texts.get(i);

      final int offset = VimPlugin.getMotion().moveCaretToLineStart(editor, line + 1);
      VimPlugin.getCopy().putText(editor, caret, context, text, SelectionType.LINE_WISE, CommandState.SubMode.NONE,
                                  offset, 1, true, false);
    }

    return true;
  }

  private int normalizeLine(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                            @NotNull ExCommand command, @NotNull LineRange lineRange) throws InvalidRangeException {
    int line = command.getRanges().getFirstLine(editor, caret, context);
    final int adj = lineRange.getEndLine() - lineRange.getStartLine() + 1;
    if (line >= lineRange.getEndLine()) line -= adj;
    else if (line >= lineRange.getStartLine()) throw new InvalidRangeException(MessageHelper.message(Msg.e_backrange));

    return line;
  }
}
