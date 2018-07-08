package com.maddyhome.idea.vim.extension.around;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author linxixin@cvte.com
 * @since 1.0
 */
public class UnAroungHandler implements VimExtensionHandler {
  @Override
  public void execute(@NotNull Editor editor, @NotNull DataContext context) {
    EditorAction editorUnSelectWord = (EditorAction)ActionManager.getInstance().getAction("EditorUnSelectWord");
    editorUnSelectWord.getHandler().execute(editor,context);
  }
}
