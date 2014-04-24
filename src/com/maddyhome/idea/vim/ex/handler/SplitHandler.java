/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
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
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class SplitHandler extends CommandHandler {
  public SplitHandler() {
    super(new CommandName[]{
      new CommandName("vs", "plit"),
      new CommandName("sp", "lit")
    }, RANGE_FORBIDDEN | ARGUMENT_OPTIONAL | DONT_REOPEN);
  }

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) {
    if (cmd.getCommand().startsWith("v")) {
      VimPlugin.getWindow().splitWindowVertical(context, cmd.getArgument());
    } else {
      VimPlugin.getWindow().splitWindowHorizontal(context, cmd.getArgument());
    }

    return true;
  }
}
