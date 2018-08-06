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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author smartbomb
 */
public class ActionListHandler extends CommandHandler {
  public ActionListHandler() {
    super("actionlist", "", RANGE_FORBIDDEN | DONT_REOPEN | ARGUMENT_OPTIONAL);
  }

  public boolean execute(@NotNull Editor editor, @NotNull final DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final String arg = cmd.getArgument().trim().toLowerCase();
    final List<String> args = StringUtil.split(arg, "*");
    final ActionManager actionManager = ActionManager.getInstance();
    final List<String> actionNames = Arrays.asList(actionManager.getActionIds(""));
    actionNames.sort(String.CASE_INSENSITIVE_ORDER);

    final StringBuilder builder = new StringBuilder();
    builder.append("--- Actions ---\n");

    for (String actionName : actionNames) {
      if (match(actionName, args)) {
        builder.append(StringHelper.leftJustify(actionName, 50, ' '));
        final AnAction action = actionManager.getAction(actionName);
        final Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
        for (Shortcut shortcut : shortcuts) {
          builder.append(" ");
          if (shortcut instanceof KeyboardShortcut) {
            final KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;
            builder.append(StringHelper.toKeyNotation(keyboardShortcut.getFirstKeyStroke()));
          }
          else {
            builder.append(shortcut.toString());
          }
        }
        builder.append("\n");
      }
    }

    ExOutputModel.getInstance(editor).output(builder.toString());
    return true;
  }

  private boolean match(@NotNull String actionName, @NotNull List<String> args) {
    for (String argChunk : args) {
      if (!actionName.toLowerCase().contains(argChunk)) {
        return false;
      }
    }
    return true;
  }
}
