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

package com.maddyhome.idea.vim.action.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.key.OperatorFunction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * @author vlan
 */
public class OperatorAction extends VimCommandAction {
  protected OperatorAction() {
    super(new EditorActionHandlerBase() {
      @Override
      protected boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
        final OperatorFunction operatorFunction = VimPlugin.getKey().getOperatorFunction();
        if (operatorFunction != null) {
          final Argument argument = cmd.getArgument();
          if (argument != null) {
            final Command motion = argument.getMotion();
            if (motion != null) {
              final TextRange range = MotionGroup
                .getMotionRange(editor, editor.getCaretModel().getPrimaryCaret(), context, cmd.getCount(),
                                cmd.getRawCount(), argument, true);
              if (range != null) {
                VimPlugin.getMark().setChangeMarks(editor, range);
                final SelectionType selectionType = SelectionType.fromCommandFlags(motion.getFlags());
                KeyHandler.getInstance().reset(editor);
                return operatorFunction.apply(editor, context, selectionType);
              }
            }
          }
          return false;
        }
        VimPlugin.showMessage(MessageHelper.message("E774"));
        return false;
      }
    });
  }

  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.N;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("g@");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.OTHER_SELF_SYNCHRONIZED;
  }

  @NotNull
  @Override
  public Argument.Type getArgumentType() {
    return Argument.Type.MOTION;
  }

  @Override
  public int getFlags() {
    return Command.FLAG_OP_PEND;
  }
}
