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

package com.maddyhome.idea.vim.action.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.maddyhome.idea.vim.key.KeyParser;
import org.jetbrains.annotations.NotNull;

/**
 */
public class RepeatChangeAction extends EditorAction {
  public RepeatChangeAction() {
    super(new Handler());
  }

  private static class Handler extends AbstractEditorActionHandler {
    public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command command) {
      CommandState state = CommandState.getInstance(editor);
      Command cmd = state.getLastChangeCommand();
      if (cmd != null) {
        if (command.getRawCount() > 0) {
          cmd.setCount(command.getCount());
          Argument arg = cmd.getArgument();
          if (arg != null) {
            Command mot = arg.getMotion();
            if (mot != null) {
              mot.setCount(0);
            }
          }
        }
        Command save = state.getCommand();
        state.setCommand(cmd);
        state.pushState(CommandState.Mode.REPEAT, CommandState.SubMode.NONE, KeyParser.MAPPING_NORMAL);
        char reg = CommandGroups.getInstance().getRegister().getCurrentRegister();
        CommandGroups.getInstance().getRegister().selectRegister(state.getLastChangeRegister());
        try {
          KeyHandler.executeAction(cmd.getAction(), context);
        }
        catch (Exception e) {
          // oops
        }
        state.popState();
        if (save != null) {
          state.setCommand(save);
        }
        state.saveLastChangeCommand(cmd);
        CommandGroups.getInstance().getRegister().selectRegister(reg);

        return true;
      }
      else {
        return false;
      }
    }
  }
}
