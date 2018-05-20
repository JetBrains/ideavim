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
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 */
public abstract class ChangeEditorActionHandler extends EditorActionHandlerBase {
  /**
   * This represents the order in which carets are given to the handlers.
   */
  public enum CaretOrder {
    /**
     * Native order in which carets are given in {@link CaretModel#getAllCarets()}
     */
    NATIVE,

    /**
     * Carets are ordered by offset, increasing
     */
    INCREASING_OFFSET,

    /**
     * Carets are ordered by offset, decreasing
     */
    DECREASING_OFFSET
  }

  private boolean myIsMulticaretChangeAction = false;
  private CaretOrder myCaretOrder;

  public ChangeEditorActionHandler(boolean runForEachCaret, CaretOrder caretOrder) {
    super(false);
    myIsMulticaretChangeAction = runForEachCaret;
    myCaretOrder = caretOrder;
  }

  public ChangeEditorActionHandler() {
    this(false, CaretOrder.NATIVE);
  }

  @Override
  protected final boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    // Here we have to save the last changed command. This should be done separately for each
    // call of the task, not for each caret. Currently there is no way to schedule any action
    // to be worked after each task. So here we override the deprecated execute function which
    // is called for each task and call the handlers for each caret, if implemented.

    boolean worked;
    if (myIsMulticaretChangeAction) {
      worked = true;
      List<Caret> carets = editor.getCaretModel().getAllCarets();
      if (myCaretOrder == CaretOrder.INCREASING_OFFSET) {
        carets.sort(Comparator.comparingInt(Caret::getOffset));
      }
      else if (myCaretOrder == CaretOrder.DECREASING_OFFSET) {
        carets.sort(Comparator.comparingInt(Caret::getOffset));
        Collections.reverse(carets);
      }
      for (Caret caret : carets) {
        if (!execute(editor, caret, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument())) {
          worked = false;
        }
      }
    }
    else {
      worked = execute(editor, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
    }
    if (worked) {
      CommandState.getInstance(editor).saveLastChangeCommand(cmd);
    }
    return worked;
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, int count, int rawCount,
                         @Nullable Argument argument) {
    return execute(editor, editor.getCaretModel().getPrimaryCaret(), context, count, rawCount, argument);
  }

  public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                         int rawCount, @Nullable Argument argument) {
    return execute(editor, context, count, rawCount, argument);
  }
}
