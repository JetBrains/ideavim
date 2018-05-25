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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public abstract class VisualOperatorActionHandler extends EditorActionHandlerBase {
  private final boolean myRunForEachCaret;
  private final CaretOrder myCaretOrder;

  public VisualOperatorActionHandler(boolean runForEachCaret, CaretOrder caretOrder) {
    super(false);
    myRunForEachCaret = runForEachCaret;
    myCaretOrder = caretOrder;
  }

  public VisualOperatorActionHandler() {
    this(false, CaretOrder.NATIVE);
  }

  @Override
  protected final boolean execute(@NotNull final Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    // As in ChangeEditorActionHandler, some actions there should also be done before and after each action (such as
    // exiting/entering/toggling visual mode, so this also overrides single-caret version.

    if (logger.isDebugEnabled()) logger.debug("execute, cmd=" + cmd);

    EditorData.setChangeSwitchMode(editor, null);
    EditorData.setWasVisualBlockMode(editor, CommandState.inVisualBlockMode(editor));

    boolean willRunForEachCaret = myRunForEachCaret && !CommandState.inVisualBlockMode(editor);

    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      TextRange range = VimPlugin.getMotion().getVisualRange(editor);
      if (logger.isDebugEnabled()) logger.debug("range=" + range);
    }

    VisualStartFinishRunnable runnable = new VisualStartFinishRunnable(editor, cmd, willRunForEachCaret);
    runnable.start();

    boolean res;
    if (willRunForEachCaret) {
      res = true;
      for (Caret caret : EditorHelper.getOrderedCaretsList(editor, myCaretOrder)) {
        TextRange range = CaretData.getVisualTextRange(caret);
        if (range == null) {
          return false;
        }
        try {
          if (!execute(editor, caret, context, cmd, range)) {
            res = false;
          }
        }
        catch (ExecuteMethodNotOverriddenException e) {
          return false;
        }
      }
    }
    else {
      TextRange range = CaretData.getVisualTextRange(editor.getCaretModel().getPrimaryCaret());
      if (range == null) {
        return false;
      }
      try {
        res = execute(editor, context, cmd, range);
      }
      catch (ExecuteMethodNotOverriddenException e) {
        return false;
      }
    }

    runnable.setRes(res);
    runnable.finish();

    CommandState.Mode toSwitch = EditorData.getChangeSwitchMode(editor);
    if (toSwitch != null) {
      VimPlugin.getChange().processPostChangeModeSwitch(editor, context, toSwitch);
    }

    return res;
  }

  protected boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd,
                            @NotNull TextRange range) throws ExecuteMethodNotOverriddenException {
    if (!myRunForEachCaret) {
      throw new ExecuteMethodNotOverriddenException(this.getClass());
    }
    return execute(editor, editor.getCaretModel().getPrimaryCaret(), context, cmd, range);
  }

  protected boolean execute(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                            @NotNull Command cmd, @NotNull TextRange range) throws ExecuteMethodNotOverriddenException {
    if (myRunForEachCaret) {
      throw new ExecuteMethodNotOverriddenException(this.getClass());
    }
    return execute(editor, context, cmd, range);
  }

  private static class VisualStartFinishRunnable {
    public VisualStartFinishRunnable(@NotNull Editor editor, Command cmd, boolean runForEachCaret) {
      this.editor = editor;
      this.cmd = cmd;
      this.res = true;
      this.myRunForEachCaret = runForEachCaret;
    }

    public void setRes(boolean res) {
      this.res = res;
    }

    void startForCaret(Caret caret) {
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPEAT) {
        CaretData.setPreviousLastColumn(caret, CaretData.getLastColumn(caret));
        VisualChange range = CaretData.getLastVisualOperatorRange(caret);
        VimPlugin.getMotion().toggleVisual(editor, 1, 1, CommandState.SubMode.NONE);
        if (range != null && range.getColumns() == MotionGroup.LAST_COLUMN) {
          CaretData.setLastColumn(editor, caret, MotionGroup.LAST_COLUMN);
        }
      }

      VisualChange change = null;
      TextRange res = null;
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        if (!myRunForEachCaret) {
          res = VimPlugin.getMotion().getVisualRange(editor);
        }
        else {
          res = VimPlugin.getMotion().getVisualRange(caret);
        }
        if (!wasRepeat) {
          change = VimPlugin.getMotion()
            .getVisualOperatorRange(editor, caret, cmd == null ? Command.FLAG_MOT_LINEWISE : cmd.getFlags());
        }
        if (logger.isDebugEnabled()) logger.debug("change=" + change);
      }
      CaretData.setVisualChange(caret, change);
      CaretData.setVisualTextRange(caret, res);
    }

    public void start() {
      logger.debug("start");
      wasRepeat = CommandState.getInstance(editor).getMode() == CommandState.Mode.REPEAT;
      EditorData.setKeepingVisualOperatorAction(editor, (cmd.getFlags() & Command.FLAG_EXIT_VISUAL) == 0);

      if (myRunForEachCaret) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          startForCaret(caret);
        }
      }
      else {
        startForCaret(editor.getCaretModel().getPrimaryCaret());
      }

      // If this is a mutli key change then exit visual now
      if (cmd != null && (cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) != 0) {
        logger.debug("multikey undo - exit visual");
        VimPlugin.getMotion().exitVisual(editor);
      }
      else if (cmd != null && (cmd.getFlags() & Command.FLAG_FORCE_LINEWISE) != 0) {
        lastMode = CommandState.getInstance(editor).getSubMode();
        if (lastMode != CommandState.SubMode.VISUAL_LINE && (cmd.getFlags() & Command.FLAG_FORCE_VISUAL) != 0) {
          VimPlugin.getMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE);
        }
      }
    }

    private void finishForCaret(Caret caret) {
      if (cmd == null ||
          ((cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0 && (cmd.getFlags() & Command.FLAG_EXPECT_MORE) == 0)) {
        if (wasRepeat) {
          CaretData.setLastColumn(editor, caret, CaretData.getPreviousLastColumn(caret));
        }
      }

      if (res) {
        @Nullable VisualChange change = CaretData.getVisualChange(caret);
        if (change != null) {
          CaretData.setLastVisualOperatorRange(caret, change);
        }
      }
    }

    public void finish() {
      logger.debug("finish");

      if (cmd != null && (cmd.getFlags() & Command.FLAG_FORCE_LINEWISE) != 0) {
        if (lastMode != CommandState.SubMode.VISUAL_LINE && (cmd.getFlags() & Command.FLAG_FORCE_VISUAL) != 0) {
          VimPlugin.getMotion().toggleVisual(editor, 1, 0, lastMode);
        }
      }

      if (cmd == null ||
          ((cmd.getFlags() & Command.FLAG_MULTIKEY_UNDO) == 0 && (cmd.getFlags() & Command.FLAG_EXPECT_MORE) == 0)) {
        logger.debug("not multikey undo - exit visual");
        VimPlugin.getMotion().exitVisual(editor);
      }

      if (res) {
        if (cmd != null) {
          CommandState.getInstance(editor).saveLastChangeCommand(cmd);
        }
      }

      if (myRunForEachCaret) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          finishForCaret(caret);
        }
      }
      else {
        finishForCaret(editor.getCaretModel().getPrimaryCaret());
      }

      EditorData.setKeepingVisualOperatorAction(editor, false);
    }

    private final Command cmd;
    private final Editor editor;
    private boolean res;
    @NotNull private CommandState.SubMode lastMode;
    private boolean wasRepeat;
    private boolean myRunForEachCaret;
  }

  private static final Logger logger = Logger.getInstance(VisualOperatorActionHandler.class.getName());
}
