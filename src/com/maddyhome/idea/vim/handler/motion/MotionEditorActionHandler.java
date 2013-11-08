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

package com.maddyhome.idea.vim.handler.motion;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

/**
 */
public abstract class MotionEditorActionHandler extends AbstractEditorActionHandler {
  protected final boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    preMove(editor, context, cmd);

    int offset = getOffset(editor, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
    if (offset == -1) {
      return false;
    }
    else if (offset >= 0) {
      if ((cmd.getFlags() & Command.FLAG_SAVE_JUMP) != 0) {
        CommandGroups.getInstance().getMark().saveJumpLocation(editor);
      }
      if (!CommandState.inInsertMode(editor) && !CommandState.inRepeatMode(editor) &&
          !CommandState.inVisualCharacterMode(editor)) {
        offset = EditorHelper.normalizeOffset(editor, offset, false);
      }
      MotionGroup.moveCaret(editor, offset);
      postMove(editor, context, cmd);

      return true;
    }
    else {
      return true;
    }
  }

  public abstract int getOffset(Editor editor, DataContext context, int count, int rawCount, Argument argument);

  protected void preMove(Editor editor, DataContext context, Command cmd) {
  }

  protected void postMove(Editor editor, DataContext context, Command cmd) {
  }
}
