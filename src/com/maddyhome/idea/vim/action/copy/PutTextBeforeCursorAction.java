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

package com.maddyhome.idea.vim.action.copy;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.group.copy.PutData;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public class PutTextBeforeCursorAction extends ChangeEditorActionHandler.SingleExecution {
  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.N;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("P");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.OTHER_SELF_SYNCHRONIZED;
  }

  @Override
  public boolean execute(@NotNull Editor editor,
                         @NotNull DataContext context,
                         int count,
                         int rawCount,
                         @Nullable Argument argument) {
    final Register lastRegister = VimPlugin.getRegister().getLastRegister();

    final PutData.TextData textData =
      lastRegister != null ? new PutData.TextData(lastRegister.getText(), lastRegister.getType(),
                                                  lastRegister.getTransferableData()) : null;
    final PutData putData = new PutData(textData, null, count, true, true, false, -1);
    return VimPlugin.getPut().putText(editor, context, putData);
  }
}
