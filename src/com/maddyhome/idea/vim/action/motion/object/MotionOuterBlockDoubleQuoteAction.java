package com.maddyhome.idea.vim.action.motion.object;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.action.motion.TextObjectAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.motion.TextObjectActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MotionOuterBlockDoubleQuoteAction extends TextObjectAction {
  public MotionOuterBlockDoubleQuoteAction() {
    super(new MotionOuterBlockDoubleQuoteAction.Handler());
  }

  private static class Handler extends TextObjectActionHandler {
    @Nullable
    public TextRange getRange(@NotNull Editor editor, DataContext context, int count, int rawCount, Argument argument) {
      return CommandGroups.getInstance().getMotion().getBlockQuoteRange(editor, '"', true);
    }
  }
}
