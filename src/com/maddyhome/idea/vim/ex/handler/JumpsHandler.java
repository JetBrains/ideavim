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
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.common.Jump;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.ui.MorePanel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 */
public class JumpsHandler extends CommandHandler {
  public JumpsHandler() {
    super(new CommandName[]{
      new CommandName("ju", "mps")
    }, ARGUMENT_FORBIDDEN | KEEP_FOCUS);
  }

  public boolean execute(@NotNull Editor editor, DataContext context, ExCommand cmd) throws ExException {
    List<Jump> jumps = CommandGroups.getInstance().getMark().getJumps();
    int spot = CommandGroups.getInstance().getMark().getJumpSpot();

    String spaces = "     ";
    StringBuffer text = new StringBuffer();
    text.append("  jump line  col file/text\n");
    int i = jumps.size();
    for (Jump jump : jumps) {
      if (i - spot - 1 == 0) {
        text.append("> ");
      }
      else {
        text.append("  ");
      }
      text.append(StringHelper.pad(Integer.toString(Math.abs(i - spot - 1)), 3, ' '));

      text.append(" ");
      String num = Integer.toString(jump.getLogicalLine() + 1);
      text.append(spaces.substring(0, 5 - num.length()));
      text.append(num);

      text.append("  ");
      num = Integer.toString(jump.getCol() + 1);
      text.append(spaces.substring(0, 3 - num.length()));
      text.append(num);

      text.append(" ");
      VirtualFile vf = EditorData.getVirtualFile(editor);
      if (vf != null && vf.getPath().equals(jump.getFilename())) {
        text.append(StringHelper.escape(EditorHelper.getLineText(editor, jump.getLogicalLine()).trim()));
      }
      else {
        text.append(jump.getFilename());
      }

      text.append("\n");
      i--;
    }

    if (spot == -1) {
      text.append(">\n");
    }

    MorePanel panel = MorePanel.getInstance(editor);
    panel.setText(text.toString());

    return true;
  }
}
