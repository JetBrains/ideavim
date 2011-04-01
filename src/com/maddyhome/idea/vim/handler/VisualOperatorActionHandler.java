package com.maddyhome.idea.vim.handler;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

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

/**
 *
 */
public abstract class VisualOperatorActionHandler extends AbstractEditorActionHandler {
  protected final boolean execute(final Editor editor, DataContext context, Command cmd) {
    if (logger.isDebugEnabled()) logger.debug("execute, cmd=" + cmd);

    TextRange range = null;
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
      range = CommandGroups.getInstance().getMotion().getVisualRange(editor);
      if (logger.isDebugEnabled()) logger.debug("range=" + range);
    }

    VisualStartFinishRunnable runnable = new VisualStartFinishRunnable(editor, context, cmd);
    if (cmd == null || (cmd.getFlags() & Command.FLAG_DELEGATE) != 0) {
      DelegateCommandListener.getInstance().setRunnable(runnable);
    }
    else {
      range = runnable.start();
    }

    boolean res = execute(editor, context, cmd, range);

    if (cmd != null && (cmd.getFlags() & Command.FLAG_DELEGATE) == 0) {
      runnable.setRes(res);
      runnable.finish();
    }

    return res;
  }

  protected abstract boolean execute(Editor editor, DataContext context, Command cmd, TextRange range);

  private static class VisualStartFinishRunnable implements DelegateCommandListener.StartFinishRunnable {
    public VisualStartFinishRunnable(Editor editor, DataContext context, Command cmd) {
      this.editor = editor;
      this.context = context;
      this.cmd = cmd;
      this.res = true;
    }

    public void setRes(boolean res) {
      this.res = res;
    }

    @SuppressWarnings("Since15")
    public TextRange start() {
      logger.debug("start");
      wasRepeat = false;
      if (CommandState.getInstance(editor).getMode() == CommandState.MODE_REPEAT) {
        wasRepeat = true;
        lastColumn = EditorData.getLastColumn(editor);
        VisualChange range = EditorData.getLastVisualOperatorRange(editor);
        CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 1, 0);
        if (range.getColumns() == MotionGroup.LAST_COLUMN) {
          EditorData.setLastColumn(editor, MotionGroup.LAST_COLUMN);
        }
      }

      TextRange res = null;
      if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
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
        CommandGroups.getInstance().getMotion().exitVisual(editor);
      }
      else if (cmd != null && (cmd.getFlags() & Command.FLAG_FORCE_LINEWISE) != 0) {
        lastMode = CommandState.getInstance(editor).getSubMode();
        if (lastMode != Command.FLAG_MOT_LINEWISE && (cmd.getFlags() & Command.FLAG_FORCE_VISUAL) != 0) {
          CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 0,
                                                               Command.FLAG_MOT_LINEWISE);
        }
      }

      return res;
    }

    public void finish() {
      logger.debug("finish");

      if (cmd != null && (cmd.getFlags() & Command.FLAG_FORCE_LINEWISE) != 0) {
        if (lastMode != Command.FLAG_MOT_LINEWISE && (cmd.getFlags() & Command.FLAG_FORCE_VISUAL) != 0) {
          CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 0, lastMode);
        }
      }

      if (cmd == null || ((cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0 &&
                          (cmd.getFlags() & Command.FLAG_EXPECT_MORE) == 0)) {
        logger.debug("not multikey undo - exit visual");
        if (cmd == null || (cmd.getFlags() & Command.FLAG_KEEP_VISUAL) == 0) {
          CommandGroups.getInstance().getMotion().exitVisual(editor);
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
    private DataContext context;
    private boolean res;
    private int lastMode;
    private boolean wasRepeat;
    private int lastColumn;
    VisualChange change = null;
  }

  private static Logger logger = Logger.getInstance(VisualOperatorActionHandler.class.getName());
}
