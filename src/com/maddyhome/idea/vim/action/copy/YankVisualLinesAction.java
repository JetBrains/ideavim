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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * @author vlan
 */
final public class YankVisualLinesAction extends VisualOperatorActionHandler.SingleExecution {
  @Contract(pure = true)
  @NotNull
  @Override
  final public Set<MappingMode> getMappingModes() {
    return MappingMode.V;
  }

  @NotNull
  @Override
  final public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("Y");
  }

  @Contract(pure = true)
  @NotNull
  @Override
  final public Command.Type getType() {
    return Command.Type.COPY;
  }

  @NotNull
  @Override
  final public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE, CommandFlags.FLAG_EXIT_VISUAL);
  }

  @Override
  final public boolean executeForAllCarets(@NotNull Editor editor,
                                           @NotNull DataContext context,
                                           @NotNull Command cmd,
                                           @NotNull Map<Caret, ? extends VimSelection> caretsAndSelections) {
    Collection<? extends VimSelection> selections = caretsAndSelections.values();
    List<Integer> starts = new ArrayList<>();
    List<Integer> ends = new ArrayList<>();
    selections.forEach(selection -> {
      TextRange textRange = selection.toVimTextRange(false);
      Arrays.stream(textRange.getStartOffsets()).boxed().forEach(starts::add);
      Arrays.stream(textRange.getEndOffsets()).boxed().forEach(ends::add);
    });
    VimSelection vimSelection = selections.stream().findFirst().orElse(null);
    if (vimSelection == null) return false;
    int[] startsArray = starts.stream().mapToInt(i -> i).toArray();
    int[] endsArray = ends.stream().mapToInt(i -> i).toArray();
    if (vimSelection.getType() == SelectionType.BLOCK_WISE) {
      return VimPlugin.getYank()
        .yankRange(editor, new TextRange(startsArray, endsArray), SelectionType.BLOCK_WISE, true);
    }
    else {
      return VimPlugin.getYank()
        .yankRange(editor, new TextRange(startsArray, endsArray), SelectionType.LINE_WISE, true);
    }
  }
}
