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
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.CaretOrder;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class GotoCharacterHandler extends CommandHandler {
  public GotoCharacterHandler() {
    super("go", "to", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | RANGE_IS_COUNT, Command.FLAG_MOT_EXCLUSIVE, true,
          CaretOrder.DECREASING_OFFSET);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final int count = cmd.getCount(editor, caret, context, 1, true);
    if (count <= 0) return false;

    final int offset = VimPlugin.getMotion().moveCaretToNthCharacter(editor, count - 1);
    if (offset == -1) return false;

    MotionGroup.moveCaret(editor, caret, offset);

    return true;
  }
}
