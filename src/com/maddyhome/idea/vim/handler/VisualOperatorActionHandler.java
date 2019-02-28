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

package com.maddyhome.idea.vim.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

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
      TextRange range = VisualMotionGroup.INSTANCE.getVisualRange(editor);
      if (logger.isDebugEnabled()) logger.debug("range=" + range);
    }

    VisualStartFinishRunnable runnable = new VisualStartFinishRunnable(editor, cmd, willRunForEachCaret);
    runnable.start();


    final Ref<Boolean> res = Ref.create(true);
    if (willRunForEachCaret) {
      editor.getCaretModel().runForEachCaret(caret -> {
        TextRange range = CaretData.getVisualTextRange(caret);
        if (range == null) {
          res.set(false);
          return;
        }
        try {
          if (!execute(editor, caret, context, cmd, range)) {
            res.set(false);
          }
        }
        catch (ExecuteMethodNotOverriddenException e) {
          res.set(false);
        }
      }, myCaretOrder == CaretOrder.DECREASING_OFFSET);
    }
    else {
      TextRange range = CaretData.getVisualTextRange(editor.getCaretModel().getPrimaryCaret());
      if (range == null) {
        return false;
      }
      try {
        res.set(execute(editor, context, cmd, range));
      }
      catch (ExecuteMethodNotOverriddenException e) {
        return false;
      }
    }

    runnable.setRes(res.get());
    runnable.finish();

    CommandState.Mode toSwitch = EditorData.getChangeSwitchMode(editor);
    if (toSwitch != null) {
      VimPlugin.getChange().processPostChangeModeSwitch(editor, context, toSwitch);
    }

    return res.get();
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
        VisualMotionGroup.INSTANCE.toggleVisual(editor, 1, 1, CommandState.SubMode.NONE);
        if (range != null && range.getColumns() == MotionGroup.LAST_COLUMN) {
          CaretData.setLastColumn(editor, caret, MotionGroup.LAST_COLUMN);
        }
      }

      VisualChange change = null;
      TextRange res = null;
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        if (!myRunForEachCaret) {
          res = VisualMotionGroup.INSTANCE.getVisualRange(editor);
        }
        else {
          res = VisualMotionGroup.INSTANCE.getVisualRange(caret);
        }
        if (!wasRepeat) {
          change = VisualMotionGroup.INSTANCE
            .getVisualOperatorRange(editor, caret, cmd == null ? EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE) : cmd.getFlags());
        }
        if (logger.isDebugEnabled()) logger.debug("change=" + change);
      }
      CaretData.setVisualChange(caret, change);
      CaretData.setVisualTextRange(caret, res);
    }

    public void start() {
      logger.debug("start");
      wasRepeat = CommandState.getInstance(editor).getMode() == CommandState.Mode.REPEAT;
      EditorData.setKeepingVisualOperatorAction(editor, !cmd.getFlags().contains(CommandFlags.FLAG_EXIT_VISUAL));

      if (myRunForEachCaret) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          startForCaret(caret);
        }
      }
      else {
        startForCaret(editor.getCaretModel().getPrimaryCaret());
      }

      // If this is a mutli key change then exit visual now
      if (cmd.getFlags().contains(CommandFlags.FLAG_MULTIKEY_UNDO)) {
        logger.debug("multikey undo - exit visual");
        VisualMotionGroup.INSTANCE.exitVisual(editor);
      }
      else if (cmd.getFlags().contains(CommandFlags.FLAG_FORCE_LINEWISE)) {
        lastMode = CommandState.getInstance(editor).getSubMode();
        if (lastMode != CommandState.SubMode.VISUAL_LINE && cmd.getFlags().contains(CommandFlags.FLAG_FORCE_VISUAL)) {
          VisualMotionGroup.INSTANCE.toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE);
        }
      }
    }

    private void finishForCaret(Caret caret) {
      if (cmd == null ||
          (!cmd.getFlags().contains(CommandFlags.FLAG_MULTIKEY_UNDO) && !cmd.getFlags().contains(CommandFlags.FLAG_EXPECT_MORE))) {
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

      if (cmd != null && cmd.getFlags().contains(CommandFlags.FLAG_FORCE_LINEWISE)) {
        if (lastMode != CommandState.SubMode.VISUAL_LINE && cmd.getFlags().contains(CommandFlags.FLAG_FORCE_VISUAL)) {
          VisualMotionGroup.INSTANCE.toggleVisual(editor, 1, 0, lastMode);
        }
      }

      if (cmd == null ||
              !cmd.getFlags().contains(CommandFlags.FLAG_MULTIKEY_UNDO) && !cmd.getFlags().contains(CommandFlags.FLAG_EXPECT_MORE)) {
        logger.debug("not multikey undo - exit visual");
        VisualMotionGroup.INSTANCE.exitVisual(editor);
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
