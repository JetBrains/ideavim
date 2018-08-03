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
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.MarkGroup;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PutLinesHandler extends CommandHandler {
  public PutLinesHandler() {
    super(new CommandName[]{
      new CommandName("pu", "t")
    }, RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    if (editor.isOneLineMode()) return false;

    final RegisterGroup registerGroup = VimPlugin.getRegister();
    final String arg = cmd.getArgument();
    if (arg.length() > 0 && !registerGroup.selectRegister(arg.charAt(0))) {
      return false;
    }
    else {
      registerGroup.selectRegister(registerGroup.getDefaultRegister());
    }

    final Register register = registerGroup.getLastRegister();
    if (register == null) return false;
    final String text = register.getText();

    final List<Integer> lines = cmd.getOrderedLines(editor, context, CaretOrder.DECREASING_OFFSET);
    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET);
    for (int i = 0; i < carets.size(); i++) {
      final Caret caret = carets.get(i);
      final int line = lines.get(i);

      int startOffset = Math.min(editor.getDocument().getTextLength(),
                                       VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true) + 1);
      if (startOffset > 0 && startOffset == editor.getDocument().getTextLength() &&
          editor.getDocument().getCharsSequence().charAt(startOffset - 1) != '\n') {
        editor.getDocument().insertString(startOffset, "\n");
        startOffset++;
      }

      if (text == null) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, startOffset));
        continue;
      }

      VimPlugin.getCopy().putText(editor, caret, context, text, SelectionType.LINE_WISE, CommandState.SubMode.NONE,
                                  startOffset, 1, false, false);
    }

    return true;
  }
}
