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
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;

import javax.swing.*;

/**
 * This handles Ex commands that just specify a range which translates to moving the cursor to the line given by the
 * range.
 */
public class SplitVerticalHandler extends CommandHandler {
  /**
   * Create the handler
   */
  public SplitVerticalHandler() {
      super(new CommandName[]{
            new CommandName("vs", "plit")
          }, ARGUMENT_FORBIDDEN | KEEP_FOCUS);
  }

  /**
   * Moves the cursor to the line entered by the user
   *
   * @param editor  The editor to perform the action in.
   * @param context The data context
   * @param cmd     The complete Ex command including range, command, and arguments
   * @return True if able to perform the command, false if not
   */
  public boolean execute(Editor editor, final DataContext context, ExCommand cmd) {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              KeyHandler.executeAction("SplitVertically", context);
          }
      });
      return true;
  }
}

