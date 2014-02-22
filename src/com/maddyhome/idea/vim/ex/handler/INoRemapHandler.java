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
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.option.iNoRemap.INoRemap;
import com.maddyhome.idea.vim.option.iNoRemap.INoRemapResult;
import com.sun.javafx.beans.annotations.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class INoRemapHandler extends CommandHandler {
  public INoRemapHandler() {
    super("ino", "remap", ARGUMENT_REQUIRED);
  }

  public boolean execute(@Nullable Editor editor, @Nullable DataContext context, @NonNull ExCommand cmd) throws ExException {
    INoRemap iNoRemap = new INoRemap(KeyParser.getInstance());
    INoRemapResult result = iNoRemap.tryToAddCustomEscape(cmd.getCommand() + " " + cmd.getArgument());
    if (result == INoRemapResult.True) {
      return true;
    }
    else if (result == INoRemapResult.False) {
      throw new ExException();
    }
    else {
      VimPlugin.showMessage(MessageHelper.message(Msg.inoremap_not_implemented));
      return false;
    }
  }
}
