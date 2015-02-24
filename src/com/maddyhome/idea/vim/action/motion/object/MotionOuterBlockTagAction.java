package com.maddyhome.idea.vim.action.motion.object;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.TextObjectAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.TextObjectActionHandler;
import org.jetbrains.annotations.NotNull;

public class MotionOuterBlockTagAction extends TextObjectAction {
  public MotionOuterBlockTagAction() {
    super(new MotionOuterBlockTagAction.Handler());
  }

  private static class Handler extends TextObjectActionHandler {
    public TextRange getRange(@NotNull Editor editor, DataContext context, int count, int rawCount, Argument argument) {
      return VimPlugin.getMotion().getTagRange(editor, count, true);
    }
  }
}
