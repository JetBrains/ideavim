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
import com.maddyhome.idea.vim.ex.*;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class SubstituteHandler extends CommandHandler {
  public SubstituteHandler() {
    super(new CommandName[]{
      new CommandName("s", "ubstitute"),
      new CommandName("&", ""),
      new CommandName("~", "")
    }, RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    boolean result = true;
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      final LineRange lineRange = cmd.getLineRange(editor, caret, context);
      if (!VimPlugin.getSearch().searchAndReplace(editor, caret, lineRange, cmd.getCommand(), cmd.getArgument())) {
        result = false;
      }
    }
    return result;
  }
}
