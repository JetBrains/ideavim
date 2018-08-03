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

package com.maddyhome.idea.vim.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public abstract class MotionEditorActionHandler extends EditorActionHandlerBase {
  public MotionEditorActionHandler() {
    this(false);
  }

  public MotionEditorActionHandler(boolean runForEachCaret) {
    super(runForEachCaret);
  }

  private void updateVisualCaretData(@NotNull Editor editor, @NotNull Caret caret) {
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      int selectionStart = caret.getSelectionStart();
      int selectionEnd = caret.getSelectionEnd();
      int caretOffset = caret.getOffset();
      if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_CHARACTER) {
        if (selectionStart == caretOffset) {
          CaretData.setVisualStart(caret, selectionEnd - 1);
          CaretData.setVisualEnd(caret, selectionStart);
        }
        else {
          CaretData.setVisualStart(caret, selectionStart);
          CaretData.setVisualEnd(caret, selectionEnd);
        }
      }
      else if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_LINE) {
        int selectionStartLine = editor.offsetToLogicalPosition(selectionStart).line;
        int selectionEndLine = editor.offsetToLogicalPosition(selectionEnd).line;
        int caretLine = editor.offsetToLogicalPosition(caretOffset).line;
        int currentVisualStartLine = editor.offsetToLogicalPosition(CaretData.getVisualStart(caret)).line;
        int currentVisualEndLine = editor.offsetToLogicalPosition(CaretData.getVisualEnd(caret)).line;
        if (selectionStartLine == caretLine) {
          if (currentVisualStartLine != selectionEndLine) {
            CaretData.setVisualStart(caret, EditorHelper.getLineEndOffset(editor, selectionEndLine, false));
          }
          if (currentVisualEndLine != caretLine) {
            CaretData.setVisualEnd(caret, caretOffset);
          }
        }
        else {
          if (currentVisualStartLine != selectionStartLine) {
            CaretData.setVisualStart(caret, EditorHelper.getLineStartOffset(editor, selectionStartLine));
          }
          if (currentVisualEndLine != caretLine) {
            CaretData.setVisualEnd(caret, caretOffset);
          }
        }
      }
    }
  }

  @Override
  protected final boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                  @NotNull Command cmd) {
    if (!CommandState.inVisualBlockMode(editor)) {
      // It comes out that merging of the overlapping selections is done by IDE. In ideavim we should be
      // able to handle such events for keeping the caret data (used by visual model) updated. There we
      // set up the caret data to the actual values.
      @NotNull Caret currentCaret;
      if (myRunForEachCaret) {
        currentCaret = caret;
      }
      else {
        currentCaret = editor.getCaretModel().getPrimaryCaret();
      }
      updateVisualCaretData(editor, currentCaret);
    }

    if (myRunForEachCaret) {
      if (CommandState.inVisualBlockMode(editor) && caret != editor.getCaretModel().getPrimaryCaret()) {
        // In visual block mode, ideavim creates multiple carets to make a selection on each line.
        // Only the primary caret of the selection should be moved though. This temporary hack
        // prevents the additional carets from being moved.
        return true;
      }
      preMove(editor, caret, context, cmd);
    }
    else {
      preMove(editor, context, cmd);
    }

    int offset;
    if (myRunForEachCaret) {
      try {
        offset = getOffset(editor, caret, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
      }
      catch (ExecuteMethodNotOverriddenException e) {
        return false;
      }
    }
    else {
      try {
        offset = getOffset(editor, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
      }
      catch (ExecuteMethodNotOverriddenException e) {
        return false;
      }
    }
    if (offset == -1) {
      return false;
    }
    else if (offset >= 0) {
      if ((cmd.getFlags() & Command.FLAG_SAVE_JUMP) != 0) {
        VimPlugin.getMark().saveJumpLocation(editor);
      }
      if (!CommandState.inInsertMode(editor) &&
          !CommandState.inRepeatMode(editor) &&
          !CommandState.inVisualCharacterMode(editor)) {
        offset = EditorHelper.normalizeOffset(editor, offset, false);
      }
      if (myRunForEachCaret) {
        MotionGroup.moveCaret(editor, caret, offset);
        postMove(editor, caret, context, cmd);
      }
      else {
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);
        postMove(editor, context, cmd);
      }

      return true;
    }
    else {
      return true;
    }
  }

  @Override
  protected final boolean execute(@NotNull Editor editor, @NotNull DataContext dataContext, @NotNull Command cmd) {
    // EditorActionHandlerBase inheritors should override the 3-arg execute if they do not run for each caret and
    // the 4-arg execute if they do. This is the 3-arg version for the "one-off" actions.
    return execute(editor, editor.getCaretModel().getPrimaryCaret(), dataContext, cmd);
  }

  public int getOffset(@NotNull Editor editor, @NotNull DataContext context, int count, int rawCount,
                       @Nullable Argument argument) throws ExecuteMethodNotOverriddenException {
    if (!myRunForEachCaret) {
      throw new ExecuteMethodNotOverriddenException(this.getClass());
    }
    return getOffset(editor, editor.getCaretModel().getPrimaryCaret(), context, count, rawCount, argument);
  }

  public int getOffset(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                       int rawCount, @Nullable Argument argument) throws ExecuteMethodNotOverriddenException {
    if (myRunForEachCaret) {
      throw new ExecuteMethodNotOverriddenException(this.getClass());
    }
    return getOffset(editor, context, count, rawCount, argument);
  }

  protected void preMove(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
  }

  protected void postMove(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
  }

  protected void preMove(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                         @NotNull Command cmd) {
  }

  protected void postMove(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                          @NotNull Command cmd) {
  }
}
