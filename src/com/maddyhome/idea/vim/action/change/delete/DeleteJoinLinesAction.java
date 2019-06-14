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

package com.maddyhome.idea.vim.action.change.delete;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DeleteJoinLinesAction extends EditorAction {
  public DeleteJoinLinesAction() {
    super(new Handler());
  }

  private static class Handler extends ChangeEditorActionHandler {
    public Handler() {
      super();
    }

    @Override
    public boolean execute(@NotNull Editor editor,
                           @NotNull DataContext context,
                           int count,
                           int rawCount,
                           @Nullable Argument argument) {
      if (editor.isOneLineMode()) return false;

      if (Options.getInstance().isSet(Options.SMARTJOIN)) {
        return VimPlugin.getChange().joinViaIdeaByCount(editor, context, count);
      }

      Ref<Boolean> res = Ref.create(true);
      editor.getCaretModel().runForEachCaret(caret -> {
        if (!VimPlugin.getChange().deleteJoinLines(editor, caret, count, false)) res.set(false);
      }, true);
      return res.get();
    }
  }
}
