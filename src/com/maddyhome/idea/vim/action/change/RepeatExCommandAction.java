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

package com.maddyhome.idea.vim.action.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import org.jetbrains.annotations.NotNull;

/**
 */
public class RepeatExCommandAction extends EditorAction {
  public RepeatExCommandAction() {
    super(new Handler());
  }

  private static class Handler extends AbstractEditorActionHandler {
    public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command command) {
      int count = command.getCount();
      try {
        return CommandParser.getInstance().processLastCommand(editor, context, count);
      }
      catch (ExException e) {
        return false;
      }
    }
  }
}
