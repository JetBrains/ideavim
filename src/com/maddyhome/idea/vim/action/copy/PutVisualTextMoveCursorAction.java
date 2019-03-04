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
import com.intellij.openapi.editor.RangeMarker;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.group.copy.PutCopyGroup;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author vlan
 */
public class PutVisualTextMoveCursorAction extends VimCommandAction {
  public PutVisualTextMoveCursorAction() {
    super(new VisualOperatorActionHandler() {

      private Register register;

      @Override
      protected boolean executeBlockwise(@NotNull Editor editor,
                                         @NotNull DataContext context,
                                         @NotNull Command cmd, @NotNull Map<Caret, ? extends RangeMarker> ranges) {
        boolean isBigP = cmd.getKeys().get(1).equals(StringHelper.parseKeys("P").get(0));
        return PutCopyGroup.INSTANCE
          .putVisualRangeBlockwise(editor, context, ranges, cmd.getCount(), true, true, register, isBigP);
      }


      @Override
      protected boolean executeCharacterAndLinewise(@NotNull Editor editor,
                                                    @NotNull Caret caret,
                                                    @NotNull DataContext context,
                                                    @NotNull Command cmd, @NotNull RangeMarker range) {
        return PutCopyGroup.INSTANCE
          .putVisualRangeCaL(editor, context, caret, range, cmd.getCount(), true, true, register);
      }

      @Override
      protected boolean beforeExecution(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
        Register register = VimPlugin.getRegister().getLastRegister();
        VimPlugin.getRegister().resetRegister();
        if (register == null) return false;
        if (register.getType() == SelectionType.LINE_WISE && editor.isOneLineMode()) return false;
        if (register.getText() == null) return false;
        this.register = register;
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
    return parseKeysSet("gp", "gP");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.PASTE;
  }

  @Override
  public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_EXIT_VISUAL);
  }
}
