package com.maddyhome.idea.vim.editor.relativenumber;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.editor.CaretListenerEditorInjector;
import com.maddyhome.idea.vim.editor.EditorGutterRefresher;
import com.maddyhome.idea.vim.option.Options;

/**
 * Enables / disables the relative line numbers feature.
 */
public class RelativeLineNumbers {

  private static RelativeLineNumbers instance;
  private RelativeLineNumbersGutterProvider editorGutterTextProvider;

  private RelativeLineNumbers() {
    editorGutterTextProvider = new RelativeLineNumbersGutterProvider();
    CaretListener caretListener = new EditorGutterRefresher(editorGutterTextProvider);
    EditorFactoryListener editorFactoryListener = new CaretListenerEditorInjector(caretListener);

    EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, ApplicationManager.getApplication());
  }

  public synchronized static RelativeLineNumbers getInstance() {
    if (instance == null) {
      instance = new RelativeLineNumbers();
    }
    return instance;
  }

  public synchronized void refresh() {
    if (VimPlugin.isEnabled() && Options.getInstance().isSet("relativenumber")) {
      editorGutterTextProvider.enabled();
    } else {
      editorGutterTextProvider.disabled();
    }
    EditorFactory.getInstance().refreshAllEditors();
  }
}
