package com.maddyhome.idea.vim.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.maddyhome.idea.vim.helper.EditorData;
import org.jetbrains.annotations.NotNull;

/**
 * Editor factory that injects a caret listener to each editor created.
 */
public class CaretListenerEditorInjector implements EditorFactoryListener {

  private final CaretListener caretListener;

  public CaretListenerEditorInjector(CaretListener caretListener) {
    this.caretListener = caretListener;
  }

  @Override
  public void editorCreated(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    if (EditorData.isFileEditor(editor)) {
      editor.getCaretModel().addCaretListener(caretListener);
    }
  }

  @Override
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    if (EditorData.isFileEditor(editor)) {
      editor.getCaretModel().removeCaretListener(caretListener);
    }
  }
}
