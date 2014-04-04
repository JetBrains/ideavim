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

package com.maddyhome.idea.vim.ex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author vlan
 */
public class VimScriptParser {
  public static final String[] VIMRC_FILES = {".ideavimrc", "_ideavimrc", ".vimrc", "_vimrc"};

  private VimScriptParser() {
  }

  @Nullable
  public static File findVimrc() {
    final String homeDirName = System.getProperty("user.home");
    if (homeDirName != null) {
      for (String fileName : VIMRC_FILES) {
        final File file = new File(homeDirName, fileName);
        if (file.exists()) {
          return file;
        }
      }
    }
    return null;
  }

  public static void executeFile(@NotNull File file) {
    try {
      final BufferedReader reader = new BufferedReader(new FileReader(file));
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          line = line.trim();
          if (line.startsWith(":")) {
            line = line.substring(1);
          }
          final CommandParser commandParser = CommandParser.getInstance();
          final ExCommand command = commandParser.parse(line);
          final CommandHandler commandHandler = commandParser.getCommandHandler(command);
          if (commandHandler instanceof VimScriptCommandHandler) {
            final VimScriptCommandHandler handler = (VimScriptCommandHandler)commandHandler;
            handler.execute(command);
          }
        }
        catch (ExException ignored) {
        }
      }
    }
    catch (IOException ignored) {
    }
  }
}
