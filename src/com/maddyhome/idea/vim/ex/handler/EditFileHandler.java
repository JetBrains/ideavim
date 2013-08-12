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
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EditFileHandler extends CommandHandler {
  public EditFileHandler() {
    super(new CommandName[]{
      new CommandName("bro", "wse"),
      new CommandName("e", "dit")
    }, RANGE_FORBIDDEN | ARGUMENT_OPTIONAL | DONT_REOPEN);
  }

  public boolean execute(@NotNull final Editor editor, @NotNull final DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final String arg = cmd.getArgument();
    if (arg != null) {
      final CommandGroups commandGroups = CommandGroups.getInstance();
      if (arg.equals("#")) {
        commandGroups.getMark().saveJumpLocation(editor);
        commandGroups.getFile().selectPreviousTab(context);
        return true;
      }
      else if (arg.length() > 0) {
        final boolean res = commandGroups.getFile().openFile(arg, context);
        if (res) {
          commandGroups.getMark().saveJumpLocation(editor);
        }
        return res;
      }
    }

    // Don't open a choose file dialog under a write action
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        KeyHandler.executeAction("OpenFile", new EditorDataContext(editor));
      }
    });

    return true;
  }
}
