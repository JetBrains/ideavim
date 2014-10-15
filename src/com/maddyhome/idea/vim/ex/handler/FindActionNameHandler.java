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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class FindActionNameHandler extends CommandHandler {
  public FindActionNameHandler() {
    super("findact", "ion", RANGE_FORBIDDEN | DONT_REOPEN | ARGUMENT_OPTIONAL);
  }

  public boolean execute(@NotNull Editor editor, @NotNull final DataContext context, @NotNull ExCommand cmd) throws ExException {
    String arg = cmd.getArgument().trim().toLowerCase();
    ActionManager aMgr = ActionManager.getInstance();
    String actionNames[] = aMgr.getActionIds("");

    StringBuilder builder = new StringBuilder();
    for (String actionName : actionNames) {
      if (actionName.toLowerCase().contains(arg)) {
        builder.append(actionName);
        AnAction action = aMgr.getAction(actionName);
        Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
        for (Shortcut shortcut : shortcuts) {
          builder.append(" " + shortcut.toString());
        }
        builder.append("\n");
      }
    }
    if (builder.length() == 0) {
      builder.append("0 results\n");
    }
    ExOutputModel.getInstance(editor).output(builder.toString());

    return true;
  }
}
