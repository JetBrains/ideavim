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

package com.maddyhome.idea.vim.action.motion.updown;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.handler.MotionEditorActionHandler;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public class MotionDownAction extends MotionEditorAction {
  public MotionDownAction() {
    super(new Handler());
  }

  private static class Handler extends MotionEditorActionHandler {
    public Handler() {
      super(true);
    }

    @Override
    public int getOffset(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                         int rawCount, @Nullable Argument argument) {
      Caret lastDownCaret = EditorData.getLastDownCaret(editor);
      EditorData.setLastDownCaret(editor, caret);
      if (CommandState.inVisualBlockMode(editor) && EditorData.shouldIgnoreNextMove(editor)) {
        EditorData.dontIgnoreNextMove(editor);
        if (lastDownCaret != caret) {
          return caret.getOffset();
        }
      }
      if (CommandState.inVisualBlockMode(editor)) {
        int blockEndOffset = EditorData.getVisualBlockEnd(editor);
        int blockStartOffset = EditorData.getVisualBlockStart(editor);
        VisualPosition blockEndPosition = editor.offsetToVisualPosition(blockEndOffset);
        VisualPosition blockStartPosition = editor.offsetToVisualPosition(blockStartOffset);
        if (blockEndPosition.getLine() < blockStartPosition.getLine()) {
          EditorData.ignoreNextMove(editor);
        }
      }

      return VimPlugin.getMotion().moveCaretVertical(editor, caret, count);
    }

    @Override
    protected void preMove(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                           @NotNull Command cmd) {
      col = CaretData.getLastColumn(caret);
    }

    @Override
    protected void postMove(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                            @NotNull Command cmd) {
      CaretData.setLastColumn(editor, caret, col);
    }

    private int col;
  }
}
