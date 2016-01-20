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
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vlan
 */
public class LetHandler extends CommandHandler implements VimScriptCommandHandler {
  private static Pattern SIMPLE_ASSIGNMENT = Pattern.compile("([A-Za-z_][A-Za-z_0-9]*)[ \\t]*=[ \\t]*(.*)");

  public LetHandler() {
    super("let", "", RANGE_FORBIDDEN | ARGUMENT_OPTIONAL);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    execute(cmd);
    return true;
  }

  @Override
  public void execute(@NotNull ExCommand cmd) throws ExException {
    final String argument = cmd.getArgument();
    if (argument.trim().isEmpty()) {
      showVariables();
    }
    else {
      final Matcher matcher = SIMPLE_ASSIGNMENT.matcher(argument);
      if (matcher.matches()) {
        final String name = matcher.group(1);
        // TODO: Check that 'name' is global
        final String expression = matcher.group(2);
        final VimScriptGlobalEnvironment env = VimScriptGlobalEnvironment.getInstance();
        final Map<String, Object> globals = env.getVariables();
        final Object value = VimScriptParser.evaluate(expression, globals);
        globals.put(name, value);
      }
      else {
        throw new ExException("Only simple '=' assignments are supported in 'let' expressions");
      }
    }
  }

  private void showVariables() throws ExException {
    throw new ExException("'let' without arguments is not supported yet");
  }
}
