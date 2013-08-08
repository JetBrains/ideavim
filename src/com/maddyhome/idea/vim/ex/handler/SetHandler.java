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
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class SetHandler extends CommandHandler {
  public SetHandler() {
    super("se", "t", ARGUMENT_OPTIONAL | KEEP_FOCUS);
  }

  public boolean execute(Editor editor, DataContext context, @NotNull ExCommand cmd) throws ExException {
    String arg = cmd.getArgument();
    if (logger.isDebugEnabled()) {
      logger.debug("arg=" + arg);
    }

    return Options.getInstance().parseOptionLine(editor, cmd.getArgument(), true);
  }

  private static Logger logger = Logger.getInstance(SetHandler.class.getName());
}
