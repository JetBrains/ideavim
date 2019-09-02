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

package com.maddyhome.idea.vim.action.change.delete;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Set;


public class DeleteJoinLinesAction extends ChangeEditorActionHandler.SingleExecution {
  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.N;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("gJ");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.DELETE;
  }

  @Override
  public boolean execute(@NotNull Editor editor,
                         @NotNull DataContext context,
                         int count,
                         int rawCount,
                         @Nullable Argument argument) {
    if (editor.isOneLineMode()) return false;

    if (OptionsManager.INSTANCE.getIdeajoin().isSet()) {
      return VimPlugin.getChange().joinViaIdeaByCount(editor, context, count);
    }
    VimPlugin.getEditor().notifyIdeaJoin(editor.getProject());

    Ref<Boolean> res = Ref.create(true);
    editor.getCaretModel().runForEachCaret(caret -> {
      if (!VimPlugin.getChange().deleteJoinLines(editor, caret, count, false)) res.set(false);
    }, true);
    return res.get();
  }
}
