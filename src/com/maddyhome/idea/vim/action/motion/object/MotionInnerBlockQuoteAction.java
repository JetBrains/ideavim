package com.maddyhome.idea.vim.action.motion.object;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.action.motion.TextObjectAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.motion.TextObjectActionHandler;

public class MotionInnerBlockQuoteAction extends TextObjectAction {
  public MotionInnerBlockQuoteAction() {
    super(new MotionInnerBlockQuoteAction.Handler());
  }

  private static class Handler extends TextObjectActionHandler {
    public TextRange getRange(Editor editor, DataContext context, int count, int rawCount, Argument argument) {
      return CommandGroups.getInstance().getMotion().getBlockQuoteRange(editor, context, false);
    }
  }
}
