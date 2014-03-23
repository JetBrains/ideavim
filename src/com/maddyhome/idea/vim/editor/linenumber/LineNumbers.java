package com.maddyhome.idea.vim.editor.linenumber;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.maddyhome.idea.vim.editor.CaretListenerEditorInjector;
import com.maddyhome.idea.vim.editor.EditorGutterRefresher;

/**
 * Refreshes the line numbers shown on the editors' text gutters.
 */
public class LineNumbers {

  private static LineNumbers instance;

  private LineNumbers() {
    LineNumbersGutterProvider editorGutterTextProvider = new LineNumbersGutterProvider();
    CaretListener caretListener = new EditorGutterRefresher(editorGutterTextProvider);
    EditorFactoryListener editorFactoryListener = new CaretListenerEditorInjector(caretListener);

    EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, ApplicationManager.getApplication());
  }

  public synchronized static LineNumbers getInstance() {
    if (instance == null) {
      instance = new LineNumbers();
    }
    return instance;
  }

  public synchronized void refresh() {
    EditorFactory.getInstance().refreshAllEditors();
  }
}
