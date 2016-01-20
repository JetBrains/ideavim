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
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;
import static com.maddyhome.idea.vim.helper.StringHelper.toKeyNotation;

/**
 *
 */
public class MarksHandler extends CommandHandler {
  public MarksHandler() {
    super(new CommandName[]{
      new CommandName("marks", "")
    }, ARGUMENT_OPTIONAL);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException {
    List<Mark> marks = VimPlugin.getMark().getMarks(editor);

    String spaces = "     ";
    StringBuilder text = new StringBuilder();
    text.append("mark  line  col file/text\n");
    for (Mark mark : marks) {
      text.append(" ");
      text.append(mark.getKey());

      text.append("   ");
      String num = Integer.toString(mark.getLogicalLine() + 1);
      text.append(spaces.substring(0, 5 - num.length()));
      text.append(num);

      text.append("  ");
      num = Integer.toString(mark.getCol() + 1);
      text.append(spaces.substring(0, 3 - num.length()));
      text.append(num);

      text.append(" ");
      VirtualFile vf = EditorData.getVirtualFile(editor);
      if (vf != null && vf.getPath().equals(mark.getFilename())) {
        text.append(toKeyNotation(stringToKeys(EditorHelper.getLineText(editor, mark.getLogicalLine()).trim())));
      }
      else {
        text.append(mark.getFilename());
      }

      text.append("\n");
    }

    ExOutputModel.getInstance(editor).output(text.toString());

    return true;
  }
}
