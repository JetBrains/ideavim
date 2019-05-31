/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.EditorData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public abstract class ChangeEditorActionHandler extends EditorActionHandlerBase {
  private boolean myIsMulticaretChangeAction;
  private CaretOrder myCaretOrder;

  public ChangeEditorActionHandler(boolean runForEachCaret, CaretOrder caretOrder) {
    super(false);
    myIsMulticaretChangeAction = runForEachCaret;
    myCaretOrder = caretOrder;
  }

  public ChangeEditorActionHandler() {
    this(false, CaretOrder.DECREASING_OFFSET);
  }

  @Override
  protected final boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    // Here we have to save the last changed command. This should be done separately for each
    // call of the task, not for each caret. Currently there is no way to schedule any action
    // to be worked after each task. So here we override the deprecated execute function which
    // is called for each task and call the handlers for each caret, if implemented.

    EditorData.setChangeSwitchMode(editor, null);

    final Ref<Boolean> worked = Ref.create(true);
    if (myIsMulticaretChangeAction) {
      editor.getCaretModel().runForEachCaret(caret -> {
        try {
          if (!execute(editor, caret, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument())) {
            worked.set(false);
          }
        }
        catch (ExecuteMethodNotOverriddenException e) {
            worked.set(false);
        }
      });
    }
    else {
      try {
        worked.set(execute(editor, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument()));
      }
      catch (ExecuteMethodNotOverriddenException e) {
        return false;
      }
    }
    if (worked.get()) {
      CommandState.getInstance(editor).saveLastChangeCommand(cmd);
    }

    CommandState.Mode toSwitch = EditorData.getChangeSwitchMode(editor);
    if (toSwitch != null) {
      VimPlugin.getChange().processPostChangeModeSwitch(editor, context, toSwitch);
    }

    return worked.get();
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, int count, int rawCount,
                         @Nullable Argument argument) throws ExecuteMethodNotOverriddenException {
    if (!myIsMulticaretChangeAction) {
      throw new ExecuteMethodNotOverriddenException(this.getClass());
    }
    return execute(editor, editor.getCaretModel().getPrimaryCaret(), context, count, rawCount, argument);
  }

  public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                         int rawCount, @Nullable Argument argument) throws ExecuteMethodNotOverriddenException {
    if (myIsMulticaretChangeAction) {
      throw new ExecuteMethodNotOverriddenException(this.getClass());
    }
    return execute(editor, context, count, rawCount, argument);
  }
}
