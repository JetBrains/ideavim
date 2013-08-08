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
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class RepeatHandler extends CommandHandler {
  public RepeatHandler() {
    super(new CommandName[]{
      new CommandName("@", "")
    }, RANGE_OPTIONAL | ARGUMENT_REQUIRED | DONT_SAVE_LAST);
  }

  public boolean execute(@NotNull Editor editor, DataContext context, @NotNull ExCommand cmd) throws ExException {
    char arg = cmd.getArgument().charAt(0);
    int line = cmd.getLine(editor, context);

    if (arg == '@') {
      arg = lastArg;
    }

    MotionGroup.moveCaret(editor, CommandGroups.getInstance().getMotion().moveCaretToLine(editor,
                                                                                                   line));
    lastArg = arg;

    if (arg == ':') {
      return CommandParser.getInstance().processLastCommand(editor, context, 1);
    }
    else {
      Register reg = CommandGroups.getInstance().getRegister().getPlaybackRegister(arg);
      if (reg != null) {
        CommandParser.getInstance().processCommand(editor, context, reg.getText(), 1);
        return true;
      }
      else {
        return false;
      }
    }
  }

  private char lastArg = ':';
}
