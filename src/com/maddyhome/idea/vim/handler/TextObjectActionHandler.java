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

package com.maddyhome.idea.vim.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public abstract class TextObjectActionHandler extends EditorActionHandlerBase {
  public TextObjectActionHandler() {
    this(false);
  }

  public TextObjectActionHandler(boolean runForEachCaret) {
    super(runForEachCaret);
  }

  @Override
  protected final boolean execute(@NotNull Editor editor, @Nullable Caret caret, @NotNull DataContext context,
                                  @NotNull Command cmd) {
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      TextRange range;
      if (myRunForEachCaret) {
        if (caret == null) {
          return false;
        }
        range = getRange(editor, caret, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
      }
      else {
        range = getRange(editor, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
      }
      if (range == null) {
        return false;
      }

      TextRange vr;
      if (myRunForEachCaret) {
        if (caret == null) {
          return false;
        }
        vr = VimPlugin.getMotion().getRawVisualRange(caret);
      }
      else {
        vr = VimPlugin.getMotion().getRawVisualRange(editor);
      }

      boolean block = (cmd.getFlags() & Command.FLAG_TEXT_BLOCK) != 0;
      int newstart = block || vr.getEndOffset() >= vr.getStartOffset() ? range.getStartOffset() : range.getEndOffset();
      int newend = block || vr.getEndOffset() >= vr.getStartOffset() ? range.getEndOffset() : range.getStartOffset();

      if (vr.getStartOffset() == vr.getEndOffset() || block) {
        if (myRunForEachCaret) {
          if (caret == null) {
            return false;
          }
          VimPlugin.getMotion().moveVisualStart(caret, newstart);
        }
        else {
          VimPlugin.getMotion().moveVisualStart(editor.getCaretModel().getPrimaryCaret(), newstart);
        }
      }

      if (((cmd.getFlags() & Command.FLAG_MOT_LINEWISE) != 0 &&
           (cmd.getFlags() & Command.FLAG_VISUAL_CHARACTERWISE) == 0) &&
          CommandState.getInstance(editor).getSubMode() != CommandState.SubMode.VISUAL_LINE) {
        VimPlugin.getMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE);
      }
      else if (((cmd.getFlags() & Command.FLAG_MOT_LINEWISE) == 0 ||
                (cmd.getFlags() & Command.FLAG_VISUAL_CHARACTERWISE) != 0) &&
               CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_LINE) {
        VimPlugin.getMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_CHARACTER);
      }

      if (myRunForEachCaret) {
        if (caret == null) {
          return false;
        }
        MotionGroup.moveCaret(editor, caret, newend);
      }
      else {
        MotionGroup.moveCaret(editor, newend);
      }
    }

    return true;
  }

  /**
   * Version for single-caret commands.
   */
  @Nullable
  public TextRange getRange(@NotNull Editor editor, @NotNull DataContext context, int count, int rawCount,
                            @Nullable Argument argument) {
    return getRange(editor, editor.getCaretModel().getPrimaryCaret(), context, count, rawCount, argument);
  }

  /**
   * Version for multi-caret commands.
   */
  @Nullable
  public TextRange getRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                            int rawCount, @Nullable Argument argument) {
    return getRange(editor, context, count, rawCount, argument);
  }
}
