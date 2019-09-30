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
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.handler.VimActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public final class ToggleRecordingAction extends VimActionHandler.SingleExecution {

  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.NV;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("q");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.OTHER_READONLY;
  }

  @NotNull
  @Override
  public Argument.Type getArgumentType() {
    return Argument.Type.CHARACTER;
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    if (!CommandState.getInstance(editor).isRecording()) {
      final Argument argument = cmd.getArgument();
      if (argument == null) {
        return false;
      }
      char reg = argument.getCharacter();
      return VimPlugin.getRegister().startRecording(editor, reg);
    }
    else {
      VimPlugin.getRegister().finishRecording(editor);

      return true;
    }
  }
}
