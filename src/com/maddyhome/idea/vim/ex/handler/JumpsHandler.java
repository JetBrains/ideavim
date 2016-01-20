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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.Jump;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.*;

/**
 *
 */
public class JumpsHandler extends CommandHandler {
  public JumpsHandler() {
    super(new CommandName[]{
      new CommandName("ju", "mps")
    }, ARGUMENT_FORBIDDEN);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException {
    List<Jump> jumps = VimPlugin.getMark().getJumps();
    int spot = VimPlugin.getMark().getJumpSpot();

    String spaces = "     ";
    StringBuilder text = new StringBuilder();
    text.append("  jump line  col file/text\n");
    int i = jumps.size();
    for (Jump jump : jumps) {
      if (i - spot - 1 == 0) {
        text.append("> ");
      }
      else {
        text.append("  ");
      }
      text.append(rightJustify(Integer.toString(Math.abs(i - spot - 1)), 3, ' '));

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
        text.append(toKeyNotation(stringToKeys(EditorHelper.getLineText(editor, jump.getLogicalLine()).trim())));
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

    ExOutputModel.getInstance(editor).output(text.toString());

    return true;
  }
}
