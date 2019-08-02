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

package com.maddyhome.idea.vim.action.change.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.VimActionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public class FilterMotionAction extends VimCommandAction {
  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.N;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("!");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.CHANGE;
  }

  @NotNull
  @Override
  public Argument.Type getArgumentType() {
    return Argument.Type.MOTION;
  }

  @NotNull
  @Override
  public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_OP_PEND);
  }

  @NotNull
  @Override
  protected VimActionHandler makeActionHandler() {
    return new VimActionHandler.SingleExecution() {
      @Override
      public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
        final Argument argument = cmd.getArgument();
        if (argument == null) {
          return false;
        }
        TextRange range = MotionGroup
          .getMotionRange(editor, editor.getCaretModel().getPrimaryCaret(), context, cmd.getCount(), cmd.getRawCount(),
                          argument, false);
        if (range == null) {
          return false;
        }

        LogicalPosition current = editor.getCaretModel().getLogicalPosition();
        LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
        LogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
        if (current.line != start.line) {
          MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), range.getStartOffset());
        }

        int count;
        if (start.line < end.line) {
          count = end.line - start.line + 1;
        }
        else {
          count = 1;
        }

        Command command = new Command(count, null, null, Command.Type.UNDEFINED, EnumSet.noneOf(CommandFlags.class));
        VimPlugin.getProcess().startFilterCommand(editor, context, command);

        return true;
      }
    };
  }
}
