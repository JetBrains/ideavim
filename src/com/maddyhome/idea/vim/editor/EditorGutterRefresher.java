package com.maddyhome.idea.vim.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;

/**
 * Caret listener that forces the gutter to be
 * repainted each time the caret changes its position.
 */
public class EditorGutterRefresher implements CaretListener {

  private final TextAnnotationGutterProvider provider;

  public EditorGutterRefresher(TextAnnotationGutterProvider provider) {
    this.provider = provider;
  }

  @Override
  public void caretPositionChanged(CaretEvent caretEvent) {
    Editor editor = caretEvent.getEditor();
    EditorGutter gutter = editor.getGutter();

    gutter.closeAllAnnotations();
    gutter.registerTextAnnotation(provider);
  }
}
