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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.copy;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.copy.YankCopyGroup;
import com.maddyhome.idea.vim.handler.VisualOperatorActionBatchHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * @author vlan
 */
public class YankVisualLinesAction extends VimCommandAction {
  public YankVisualLinesAction() {
    super(new VisualOperatorActionBatchHandler() {
      @Override
      public boolean executeForAllCarets(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
        int[] starts = new int[editor.getCaretModel().getCaretCount()];
        int[] ends = new int[editor.getCaretModel().getCaretCount()];
        int pointer = 0;
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          starts[pointer] = caret.getSelectionStart();
          ends[pointer] = caret.getSelectionEnd();
          pointer++;
        }
        final CommandState.SubMode mode = CommandState.getInstance(editor).getSubMode();
        if (mode == CommandState.SubMode.VISUAL_BLOCK) {
          return YankCopyGroup.INSTANCE
            .yankRange(editor, new TextRange(starts, ends), SelectionType.fromSubMode(mode), true);
        }
        else {
          return YankCopyGroup.INSTANCE.yankRange(editor, new TextRange(starts, ends), SelectionType.LINE_WISE, true);
        }
      }
    });
  }

  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.V;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("Y");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.COPY;
  }

  @Override
  public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE, CommandFlags.FLAG_EXIT_VISUAL);
  }
}
