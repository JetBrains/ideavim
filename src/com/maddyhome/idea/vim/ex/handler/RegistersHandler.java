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
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 */
public class RegistersHandler extends CommandHandler {
  public RegistersHandler() {
    super(new CommandName[]{
      new CommandName("di", "splay"),
      new CommandName("reg", "isters")
    }, ARGUMENT_OPTIONAL);
  }

  public boolean execute(@NotNull final Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) throws ExException {
    List<Register> registers = VimPlugin.getRegister().getRegisters();

    StringBuffer text = new StringBuffer();
    text.append("--- Registers ---\n");
    for (Register reg : registers) {
      text.append("\"");
      text.append(reg.getName());

      text.append("   ");
      text.append(StringHelper.escape(reg.getKeys()));
      text.append("\n");
    }

    ExOutputModel.getInstance(editor).output(text.toString());

    return true;
  }
}
