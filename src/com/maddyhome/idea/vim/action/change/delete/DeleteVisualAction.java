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

package com.maddyhome.idea.vim.action.change.delete;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * @author vlan
 */
public class DeleteVisualAction extends VimCommandAction {
  public DeleteVisualAction() {
    super(new VisualOperatorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      protected boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                @NotNull Command cmd, @NotNull TextRange range) {
        final CommandState.SubMode mode = CommandState.getInstance(editor).getSubMode();
        if (mode == CommandState.SubMode.VISUAL_LINE) {
          final TextRange lineRange = new TextRange(EditorHelper.getLineStartForOffset(editor, range.getStartOffset()),
                                                    EditorHelper.getLineEndForOffset(editor, range.getEndOffset()) + 1);
          return VimPlugin.getChange().deleteRange(editor, caret, lineRange, SelectionType.fromSubMode(mode), false);
        }
        else {
          boolean isDeleted = VimPlugin.getChange().deleteRange(editor, caret, range, SelectionType.fromSubMode(mode), false);
          if( isDeleted ) {
            LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
            CaretData.setLastColumn(editor, caret, start.column);
          }
          return isDeleted;
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
    return parseKeysSet("d", "x", "<Del>");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.DELETE;
  }

  @Override
  public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_EXIT_VISUAL);
  }
}
