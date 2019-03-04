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

package com.maddyhome.idea.vim.action.change.change.number;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import com.maddyhome.idea.vim.helper.UtilsKt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChangeVisualNumberAvalancheDecAction extends VimCommandAction {
  protected ChangeVisualNumberAvalancheDecAction() {
    super(new VisualOperatorActionHandler() {
      @Override
      protected boolean executeCharacterAndLinewise(@NotNull Editor editor,
                                                    @NotNull Caret caret,
                                                    @NotNull DataContext context,
                                                    @NotNull Command cmd,
                                                    @NotNull RangeMarker range) {
        return VimPlugin.getChange()
          .changeNumberVisualMode(editor, caret, UtilsKt.getVimTextRange(range), -cmd.getCount(), true);
      }

      @Override
      protected boolean executeBlockwise(@NotNull Editor editor,
                                         @NotNull DataContext context,
                                         @NotNull Command cmd,
                                         @NotNull Map<Caret, ? extends RangeMarker> ranges) {
        return VimPlugin.getChange()
          .changeNumberVisualMode(editor, editor.getCaretModel().getPrimaryCaret(), UtilsKt.getVimTextRange(ranges),
                                  -cmd.getCount(), true);
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
    return parseKeysSet("g<C-X>");
  }

  @NotNull
  @Override
  public Command.Type getType() {
    return Command.Type.CHANGE;
  }

  @Override
  public EnumSet<CommandFlags> getFlags() {
    return EnumSet.of(CommandFlags.FLAG_EXIT_VISUAL);
  }
}
