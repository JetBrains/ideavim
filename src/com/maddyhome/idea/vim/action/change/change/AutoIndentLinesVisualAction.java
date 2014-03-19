package com.maddyhome.idea.vim.action.change.change;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public class AutoIndentLinesVisualAction extends EditorAction {
  public AutoIndentLinesVisualAction() {
    super(new VisualOperatorActionHandler() {
      @Override
      protected boolean execute(@NotNull Editor editor,
                                @NotNull DataContext context,
                                @NotNull Command cmd,
                                @NotNull TextRange range) {
        VimPlugin.getChange().autoIndentLines(context);
        return true;
      }
    });
  }
}
