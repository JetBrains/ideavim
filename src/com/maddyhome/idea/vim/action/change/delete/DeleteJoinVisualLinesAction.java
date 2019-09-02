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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author vlan
 */
final public class DeleteJoinVisualLinesAction extends VisualOperatorActionHandler.SingleExecution {
  @Contract(pure = true)
  @NotNull
  @Override
  final public Set<MappingMode> getMappingModes() {
    return MappingMode.V;
  }

  @NotNull
  @Override
  final public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("gJ");
  }

  @Contract(pure = true)
  @NotNull
  @Override
  final public Command.Type getType() {
    return Command.Type.DELETE;
  }

  @Contract(pure = true)
  @NotNull
  @Override
  final public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_EXIT_VISUAL);
  }

  @Override
  public boolean executeForAllCarets(@NotNull Editor editor,
                                     @NotNull DataContext context,
                                     @NotNull Command cmd,
                                     @NotNull Map<Caret, ? extends VimSelection> caretsAndSelections) {
    if (editor.isOneLineMode()) return false;

    if (OptionsManager.INSTANCE.getIdeajoin().isSet()) {
      VimPlugin.getChange().joinViaIdeaBySelections(editor, context, caretsAndSelections);
      return true;
    }

    Ref<Boolean> res = Ref.create(true);
    editor.getCaretModel().runForEachCaret(caret -> {
      if (!caret.isValid()) return;
      final VimSelection range = caretsAndSelections.get(caret);
      if (range == null) return;

      if (!VimPlugin.getChange().deleteJoinRange(editor, caret, range.toVimTextRange(true).normalize(), false)) {
        res.set(false);
      }
    }, true);
    return res.get();
  }
}
