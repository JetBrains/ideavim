package com.maddyhome.idea.vim.action.change.delete;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import com.maddyhome.idea.vim.helper.EditorHelper;

/**
 */
public class DeleteVisualLinesAction extends EditorAction {
  public DeleteVisualLinesAction() {
    super(new Handler());
  }

  private static class Handler extends VisualOperatorActionHandler {
    protected boolean execute(Editor editor, DataContext context, Command cmd, TextRange range) {
      CommandState.SubMode mode = CommandState.getInstance(editor).getSubMode();
      if (mode == CommandState.SubMode.VISUAL_BLOCK) {
        return CommandGroups.getInstance().getChange().deleteRange(editor, range,
                                                                   SelectionType.fromSubMode(mode));
      }
      else {
        range = new TextRange(EditorHelper.getLineStartForOffset(editor, range.getStartOffset()),
                              EditorHelper.getLineEndForOffset(editor, range.getEndOffset()) + 1);

        return CommandGroups.getInstance().getChange().deleteRange(editor, range, SelectionType.LINE_WISE);
      }
    }
  }
}
