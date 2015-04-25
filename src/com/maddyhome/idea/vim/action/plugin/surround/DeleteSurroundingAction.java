package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dhleong
 */
public class DeleteSurroundingAction extends EditorAction {
  protected DeleteSurroundingAction() {
    super(new Handler());
  }

  private static class Handler extends ChangeEditorActionHandler {
    @Override
    public boolean execute(@NotNull Editor editor,
                           @NotNull DataContext context,
                           int count,
                           int rawCount,
                           @Nullable Argument argument) {

      if (argument == null) {
        return false;
      }

      KeyHandler.getInstance().reset(editor);

      // In a lot of cases, vim-surround seems to
      //  implement this by using di<c> to get the
      //  part to save, then basically da<c>P
      final char chKey = argument.getCharacter();
      return SurroundingChanger.change(editor, chKey, null);
    }
  }
}
