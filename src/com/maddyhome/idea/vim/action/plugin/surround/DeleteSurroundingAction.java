package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
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

      // easy; deletion is changing the surroundings to be nothing
      final char chKey = argument.getCharacter();
      return SurroundingChanger.change(editor, chKey, null);
    }
  }
}
