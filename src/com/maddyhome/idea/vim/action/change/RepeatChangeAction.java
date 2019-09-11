/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.handler.VimActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;


public class RepeatChangeAction extends VimActionHandler.SingleExecution {
  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.N;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet(".");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.OTHER_WRITABLE;
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command command) {
    CommandState state = CommandState.getInstance(editor);
    Command cmd = state.getLastChangeCommand();

    if (cmd != null) {
      if (command.getRawCount() > 0) {
        cmd.setCount(command.getCount());
        Argument arg = cmd.getArgument();
        if (arg != null) {
          Command mot = arg.getMotion();
          mot.setCount(0);
        }
      }
      Command save = state.getCommand();
      int lastFTCmd = VimPlugin.getMotion().getLastFTCmd();
      char lastFTChar = VimPlugin.getMotion().getLastFTChar();

      state.setCommand(cmd);
      state.pushState(CommandState.Mode.REPEAT, CommandState.SubMode.NONE, MappingMode.NORMAL);
      char reg = VimPlugin.getRegister().getCurrentRegister();
      VimPlugin.getRegister().selectRegister(state.getLastChangeRegister());
      try {
        KeyHandler.executeVimAction(editor, cmd.getAction(), context);
      }
      catch (Exception e) {
        // oops
      }
      state.popState();
      if (save != null) {
        state.setCommand(save);
      }
      VimPlugin.getMotion().setLastFTCmd(lastFTCmd, lastFTChar);
      state.saveLastChangeCommand(cmd);
      VimPlugin.getRegister().selectRegister(reg);

      return true;
    }
    else {
      return false;
    }
  }
}
