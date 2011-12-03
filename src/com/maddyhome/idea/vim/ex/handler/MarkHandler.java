package com.maddyhome.idea.vim.ex.handler;

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
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;

/**
 *
 */
public class MarkHandler extends CommandHandler {
  public MarkHandler() {
    super(new CommandName[]{
      new CommandName("ma", "rk"),
      new CommandName("k", "")
    }, RANGE_OPTIONAL | ARGUMENT_REQUIRED);
  }

  public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException {
    char mark = cmd.getArgument().charAt(0);
    int line = cmd.getLine(editor, context);
    int offset = EditorHelper.getLineStartOffset(editor, line);

    if (Character.isLetter(mark) || "'`".indexOf(mark) != -1) {
      return CommandGroups.getInstance().getMark().setMark(editor, context, mark, offset);
    }
    else {
      VimPlugin.showMessage(MessageHelper.message(Msg.E191));
      return false;
    }
  }
}
