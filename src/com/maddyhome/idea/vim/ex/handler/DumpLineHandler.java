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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.*;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class DumpLineHandler extends CommandHandler {
  public DumpLineHandler() {
    super(new CommandName[]{
      new CommandName("dump", "line")
    }, RANGE_OPTIONAL);
  }

  public boolean execute(@NotNull Editor editor, DataContext context, @NotNull ExCommand cmd) throws ExException {
    LineRange range = cmd.getLineRange(editor, context, false);

    CharSequence chars = editor.getDocument().getCharsSequence();
    for (int l = range.getStartLine(); l <= range.getEndLine(); l++) {
      int start = editor.getDocument().getLineStartOffset(l);
      int end = editor.getDocument().getLineEndOffset(l);

      if (logger.isDebugEnabled()) {
        logger.debug("Line " + l + ", start offset=" + start + ", end offset=" + end);

        for (int i = start; i <= end; i++) {
          logger.debug("Offset " + i + ", char=" + chars.charAt(i) + ", lp=" + editor.offsetToLogicalPosition(i) +
                       ", vp=" + editor.offsetToVisualPosition(i));
        }
      }
    }

    return true;
  }

  private static Logger logger = Logger.getInstance(DumpLineHandler.class.getName());
}
