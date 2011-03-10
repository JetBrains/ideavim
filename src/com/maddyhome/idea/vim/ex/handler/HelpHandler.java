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
import com.intellij.openapi.help.HelpManager;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;

/**
 *
 */
public class HelpHandler extends CommandHandler {
  public HelpHandler() {
    super("h", "elp", ARGUMENT_OPTIONAL | DONT_REOPEN);
  }

  public boolean execute(Editor editor, DataContext context, ExCommand cmd) throws ExException {
    String key = cmd.getArgument();
    if (key.length() == 0) {
      key = "help.txt";
    }
    else if ("*".equals(key)) {
      key = "star";
    }

    HelpManager mgr = HelpManager.getInstance();
    mgr.invokeHelp("vim." + encode(key));

    return true;
  }

  private static String encode(String key) {
    StringBuffer res = new StringBuffer();
    for (int i = 0; i < key.length(); i++) {
      if ("%\"~<>=#&?/.".indexOf(key.charAt(i)) >= 0) {
        res.append('%').append(Integer.toHexString((int)key.charAt(i)).toUpperCase());
      }
      else {
        res.append(key.charAt(i));
      }
    }

    return res.toString();
  }
}
