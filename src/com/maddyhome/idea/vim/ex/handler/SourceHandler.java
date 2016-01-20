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
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author vlan
 */
public class SourceHandler extends CommandHandler implements VimScriptCommandHandler {
  public SourceHandler() {
    super("so", "urce", RANGE_FORBIDDEN | ARGUMENT_REQUIRED);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException {
    execute(cmd);
    return true;
  }

  @Override
  public void execute(@NotNull ExCommand cmd) throws ExException {
    final String path = expandUser(cmd.getArgument().trim());
    VimScriptParser.executeFile(new File(path));
  }

  @NotNull
  private static String expandUser(@NotNull String path) {
    if (path.startsWith("~")) {
      final String home = System.getProperty("user.home");
      if (home != null) {
        path = home + path.substring(1);
      }
    }
    return path;
  }
}
