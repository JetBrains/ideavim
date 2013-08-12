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

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

public class PutLinesHandler extends CommandHandler {
  public PutLinesHandler() {
    super(new CommandName[]{
      new CommandName("pu", "t")
    }, RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final CommandGroups groups = CommandGroups.getInstance();
    final RegisterGroup registerGroup = groups.getRegister();
    final int line = cmd.getLine(editor, context);
    final String arg = cmd.getArgument();

    if (arg.length() > 0) {
      if (!registerGroup.selectRegister(arg.charAt(0))) {
        return false;
      }
    }
    else {
      registerGroup.selectRegister(RegisterGroup.REGISTER_DEFAULT);
    }

    final int offset = EditorHelper.getLineStartOffset(editor, line + 1);
    editor.getDocument().insertString(offset, "\n");
    MotionGroup.moveCaret(editor, offset);
    final boolean result = groups.getCopy().putTextAfterCursor(editor, context, 1, false, false);
    final int newOffset = EditorHelper.getLineStartOffset(editor, line + 1);
    MotionGroup.moveCaret(editor, newOffset);
    return result;
  }
}
