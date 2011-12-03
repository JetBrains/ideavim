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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.Ranges;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;

import java.io.IOException;

/**
 *
 */
public class CmdFilterHandler extends CommandHandler {
  public CmdFilterHandler() {
    super("!", "", RANGE_REQUIRED | ARGUMENT_OPTIONAL | WRITABLE);
  }

  public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException {
    logger.info("execute");

    Ranges ranges = cmd.getRanges();
    if (ranges.size() == 0) {
      // Need some range
      return false;
    }
    else {
      // Filter
      TextRange range = cmd.getTextRange(editor, context, false);
      String command = cmd.getArgument();
      if (command.indexOf('!') != -1) {
        String last = CommandGroups.getInstance().getProcess().getLastCommand();
        if (last == null || last.length() == 0) {
          VimPlugin.showMessage(MessageHelper.message(Msg.e_noprev));
          return false;
        }

        command = command.replaceAll("!", last);
      }

      if (command == null || command.length() == 0) {
        return false;
      }

      try {
        return CommandGroups.getInstance().getProcess().executeFilter(editor, context, range, command);
      }
      catch (IOException e) {
        throw new ExException(e.getMessage());
      }
    }
  }

  private static Logger logger = Logger.getInstance(CmdFilterHandler.class.getName());
}
