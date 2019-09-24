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

package com.maddyhome.idea.vim.action.motion.leftright;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.MotionActionHandler;
import com.maddyhome.idea.vim.helper.CommandStateHelper;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.option.BoundStringOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public class MotionLastScreenColumnAction extends MotionActionHandler.ForEachCaret {
  @NotNull
  @Override
  public Set<MappingMode> getMappingModes() {
    return MappingMode.NVO;
  }

  @NotNull
  @Override
  public Set<List<KeyStroke>> getKeyStrokesSet() {
    return parseKeysSet("g$", "g<End>");
  }

  @Override
  public int getOffset(@NotNull Editor editor,
                       @NotNull Caret caret,
                       @NotNull DataContext context,
                       int count,
                       int rawCount,
                       Argument argument) {
    boolean allow = false;
    if (CommandStateHelper.inInsertMode(editor)) {
      allow = true;
    }
    else if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      BoundStringOption opt = OptionsManager.INSTANCE.getSelection();
      if (!opt.getValue().equals("old")) {
        allow = true;
      }
    }

    return VimPlugin.getMotion().moveCaretToLineScreenEnd(editor, caret, allow);
  }

  @Override
  public void postMove(@NotNull Editor editor,
                       @NotNull Caret caret,
                       @NotNull DataContext context,
                       @NotNull Command cmd) {
    UserDataManager.setVimLastColumn(caret, MotionGroup.LAST_COLUMN);
  }

  @NotNull
  @Override
  public MotionType getMotionType() {
    return MotionType.INCLUSIVE;
  }
}
