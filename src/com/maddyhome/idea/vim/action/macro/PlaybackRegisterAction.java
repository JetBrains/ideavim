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

package com.maddyhome.idea.vim.action.macro;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.handler.VimActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public class PlaybackRegisterAction extends VimActionHandler.SingleExecution {
  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.N;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("@");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.OTHER_SELF_SYNCHRONIZED;
  }

  @NotNull
  @Override
  public Argument.Type getArgumentType() {
    return Argument.Type.CHARACTER;
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    final Argument argument = cmd.getArgument();
    if (argument == null) {
      return false;
    }
    final char reg = argument.getCharacter();
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    Application application = ApplicationManager.getApplication();
    Ref<Boolean> res = Ref.create(false);

    if (reg == '@') {
      application.runWriteAction(
        () -> res.set(VimPlugin.getMacro().playbackLastRegister(editor, context, project, cmd.getCount())));
    }
    else if (reg == ':') {
      // No write action
      try {
        res.set(CommandParser.getInstance().processLastCommand(editor, context, cmd.getCount()));
      }
      catch (ExException e) {
        res.set(false);
      }
    }
    else {
      application.runWriteAction(
        () -> res.set(VimPlugin.getMacro().playbackRegister(editor, context, project, reg, cmd.getCount())));
    }

    return res.get();
  }
}
