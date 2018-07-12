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

package com.maddyhome.idea.vim.action.change.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * @author vlan
 */
public class AutoIndentLinesVisualAction extends VimCommandAction {
  public AutoIndentLinesVisualAction() {
    super(new VisualOperatorActionHandler(true, CaretOrder.DECREASING_OFFSET) {
      @Override
      protected boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                @NotNull Command cmd, @NotNull TextRange range) {
        VimPlugin.getChange().autoIndentRange(editor, caret, context, range);
        return true;
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
    return parseKeysSet("=");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.CHANGE;
  }

  @Override
  public int getFlags() {
    return Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE | Command.FLAG_EXIT_VISUAL;
  }
}
