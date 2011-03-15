package com.maddyhome.idea.vim.action.motion.tabs;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.motion.MotionEditorActionHandler;

/**
 * @author oleg
 */
public class MotionPreviousTabAction extends MotionEditorAction {
  public MotionPreviousTabAction() {
    super(new Handler());
  }

  private static class Handler extends MotionEditorActionHandler {
    public int getOffset(final Editor editor, final DataContext context, final int count, final int rawCount, final Argument argument) {
      return CommandGroups.getInstance().getMotion().moveCaretGotoPreviousTab(context);
    }
  }
}
