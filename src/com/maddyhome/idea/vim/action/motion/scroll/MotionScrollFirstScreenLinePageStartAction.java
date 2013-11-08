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

package com.maddyhome.idea.vim.action.motion.scroll;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

/**
 */
public class MotionScrollFirstScreenLinePageStartAction extends EditorAction {
  public MotionScrollFirstScreenLinePageStartAction() {
    super(new Handler());
  }

  private static class Handler extends AbstractEditorActionHandler {
    protected boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
      int raw = cmd.getRawCount();
      int cnt = cmd.getCount();
      if (raw == 0) {
        int lines = EditorHelper.getScreenHeight(editor);

        return CommandGroups.getInstance().getMotion().scrollLine(editor, lines);
      }
      else {
        return CommandGroups.getInstance().getMotion().scrollLineToFirstScreenLine(editor, raw, cnt, true);
      }
    }
  }
}
