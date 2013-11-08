/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.DelegateCommandListener;
import com.maddyhome.idea.vim.helper.EditorData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public abstract class VisualOperatorActionHandler extends AbstractEditorActionHandler {
  protected final boolean execute(@NotNull final Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    if (logger.isDebugEnabled()) logger.debug("execute, cmd=" + cmd);

    TextRange range = null;
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      range = CommandGroups.getInstance().getMotion().getVisualRange(editor);
      if (logger.isDebugEnabled()) logger.debug("range=" + range);
    }

    VisualStartFinishRunnable runnable = new VisualStartFinishRunnable(editor, cmd);
    if ((cmd.getFlags() & Command.FLAG_DELEGATE) != 0) {
      DelegateCommandListener.getInstance().setRunnable(runnable);
    }
    else {
      range = runnable.start();
    }

    assert range != null : "Range must be not null for visual operator action " + getClass();

    final boolean res = execute(editor, context, cmd, range);

    if ((cmd.getFlags() & Command.FLAG_DELEGATE) == 0) {
      runnable.setRes(res);
      runnable.finish();
    }

    return res;
  }

  protected abstract boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd,
                                     @NotNull TextRange range);

  private static class VisualStartFinishRunnable implements DelegateCommandListener.StartFinishRunnable {
    public VisualStartFinishRunnable(Editor editor, Command cmd) {
      this.editor = editor;
      this.cmd = cmd;
      this.res = true;
    }

    public void setRes(boolean res) {
      this.res = res;
    }

    @Nullable
    @SuppressWarnings("Since15")
    public TextRange start() {
      logger.debug("start");
      wasRepeat = false;
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPEAT) {
        wasRepeat = true;
        lastColumn = EditorData.getLastColumn(editor);
        VisualChange range = EditorData.getLastVisualOperatorRange(editor);
        CommandGroups.getInstance().getMotion().toggleVisual(editor, 1, 1, CommandState.SubMode.NONE);
        if (range.getColumns() == MotionGroup.LAST_COLUMN) {
          EditorData.setLastColumn(editor, MotionGroup.LAST_COLUMN);
        }
      }

      TextRange res = null;
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        res = CommandGroups.getInstance().getMotion().getVisualRange(editor);
        if (!wasRepeat) {
          change = CommandGroups.getInstance().getMotion().getVisualOperatorRange(editor,
                                                                                  cmd == null ? Command.FLAG_MOT_LINEWISE : cmd.getFlags());
        }
        if (logger.isDebugEnabled()) logger.debug("change=" + change);
      }

      // If this is a mutli key change then exit visual now
      if (cmd != null && (cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) != 0) {
        logger.debug("multikey undo - exit visual");
        CommandGroups.getInstance().getMotion().exitVisual(editor, true);
      }
      else if (cmd != null && (cmd.getFlags() & Command.FLAG_FORCE_LINEWISE) != 0) {
        lastMode = CommandState.getInstance(editor).getSubMode();
        if (lastMode != CommandState.SubMode.VISUAL_LINE && (cmd.getFlags() & Command.FLAG_FORCE_VISUAL) != 0) {
          CommandGroups.getInstance().getMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE);
        }
      }

      return res;
    }

    public void finish() {
      logger.debug("finish");

      if (cmd != null && (cmd.getFlags() & Command.FLAG_FORCE_LINEWISE) != 0) {
        if (lastMode != CommandState.SubMode.VISUAL_LINE && (cmd.getFlags() & Command.FLAG_FORCE_VISUAL) != 0) {
          CommandGroups.getInstance().getMotion().toggleVisual(editor, 1, 0, lastMode);
        }
      }

      if (cmd == null || ((cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0 &&
                          (cmd.getFlags() & Command.FLAG_EXPECT_MORE) == 0)) {
        logger.debug("not multikey undo - exit visual");
        if (cmd == null || (cmd.getFlags() & Command.FLAG_KEEP_VISUAL) == 0) {
          CommandGroups.getInstance().getMotion().exitVisual(editor, true);
        }
        if (wasRepeat) {
          EditorData.setLastColumn(editor, lastColumn);
        }
      }

      if (res) {
        logger.debug("res");
        if (change != null) {
          EditorData.setLastVisualOperatorRange(editor, change);
        }

        if (cmd != null) {
          CommandState.getInstance(editor).saveLastChangeCommand(cmd);
        }
      }

      if (cmd != null && (cmd.getFlags() & Command.FLAG_DELEGATE) != 0) {
        KeyHandler.getInstance().reset(editor);
      }
    }

    private Command cmd;
    private Editor editor;
    private boolean res;
    @NotNull private CommandState.SubMode lastMode;
    private boolean wasRepeat;
    private int lastColumn;
    @Nullable VisualChange change = null;
  }

  private static Logger logger = Logger.getInstance(VisualOperatorActionHandler.class.getName());
}
