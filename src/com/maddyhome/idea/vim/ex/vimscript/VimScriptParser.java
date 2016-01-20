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

package com.maddyhome.idea.vim.ex.vimscript;

import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vlan
 */
public class VimScriptParser {
  public static final String[] VIMRC_FILES = {".ideavimrc", "_ideavimrc"};
  public static final int BUFSIZE = 4096;
  private static final Pattern EOL_SPLIT_PATTERN = Pattern.compile(" *(\r\n|\n)+ *");
  private static final Pattern DOUBLE_QUOTED_STRING = Pattern.compile("\"([^\"]*)\"");
  private static final Pattern SINGLE_QUOTED_STRING = Pattern.compile("'([^']*)'");
  private static final Pattern REFERENCE_EXPR = Pattern.compile("([A-Za-z_][A-Za-z_0-9]*)");

  private VimScriptParser() {
  }

  @Nullable
  public static File findIdeaVimRc() {
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
    final String data;
    try {
      data = readFile(file);
    }
    catch (IOException ignored) {
      return;
    }
    executeText(data);
  }

  public static void executeText(@NotNull String text) {
    for (String line : EOL_SPLIT_PATTERN.split(text)) {
      // TODO: Build a proper parse tree for a VimL file instead of ignoring potentially nested lines (VIM-669)
      if (line.startsWith(" ") || line.startsWith("\t")) {
        continue;
      }
      if (line.startsWith(":")) {
        line = line.substring(1);
      }
      final CommandParser commandParser = CommandParser.getInstance();
      try {
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

  @NotNull
  public static Object evaluate(@NotNull String expression, @NotNull Map<String, Object> globals) throws ExException {
    // This evaluator is very basic, no proper parsing whatsoever. It is here as the very first step necessary to
    // support mapleader, VIM-650. See also VIM-669.
    Matcher m;
    m = DOUBLE_QUOTED_STRING.matcher(expression);
    if (m.matches()) {
      return m.group(1);
    }
    m = SINGLE_QUOTED_STRING.matcher(expression);
    if (m.matches()) {
      return m.group(1);
    }
    m = REFERENCE_EXPR.matcher(expression);
    if (m.matches()) {
      final String name = m.group(1);
      final Object value = globals.get(name);
      if (value != null) {
        return value;
      }
      else {
        throw new ExException(String.format("Undefined variable: %s", name));
      }
    }
    throw new ExException(String.format("Invalid expression: %s", expression));
  }

  @NotNull
  public static String expressionToString(@NotNull Object value) throws ExException {
    // TODO: Return meaningful value representations
    if (value instanceof String) {
      return (String)value;
    }
    throw new ExException(String.format("Cannot convert '%s' to string", value));
  }

  @NotNull
  private static String readFile(@NotNull File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    final StringBuilder builder = new StringBuilder();
    final char[] buffer = new char[BUFSIZE];
    int n;
    while ((n = reader.read(buffer)) > 0) {
      builder.append(buffer, 0, n);
    }
    return builder.toString();
  }
}
