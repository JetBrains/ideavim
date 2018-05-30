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

package com.maddyhome.idea.vim.action.motion.leftright;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.MotionEditorActionHandler;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.option.BoundStringOption;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public class MotionLastColumnAction extends MotionEditorAction {
  public MotionLastColumnAction() {
    super(new Handler());
  }

  private static class Handler extends MotionEditorActionHandler {
    public Handler() {
      super(true);
    }

    @Override
    public int getOffset(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                         int rawCount, @Nullable Argument argument) {
      boolean allow = false;
      if (CommandState.inInsertMode(editor)) {
        allow = true;
      }
      else if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
        if (!opt.getValue().equals("old")) {
          allow = true;
        }
      }

      return VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, allow);
    }

    protected void postMove(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                            @NotNull Command cmd) {
      CaretData.setLastColumn(editor, caret, MotionGroup.LAST_COLUMN);
    }
  }
}

