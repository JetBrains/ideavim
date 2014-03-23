package com.maddyhome.idea.vim.editor.linenumber;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorFontType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * Null implementation for TextAnnotationGutterProvider.
 */
public class NullGutterProvider implements TextAnnotationGutterProvider {
  @Nullable
  @Override
  public String getLineText(int i, Editor editor) {
    return null;
  }

  @Nullable
  @Override
  public String getToolTip(int i, Editor editor) {
    return null;
  }

  @Override
  public EditorFontType getStyle(int i, Editor editor) {
    return null;
  }

  @Nullable
  @Override
  public ColorKey getColor(int i, Editor editor) {
    return null;
  }

  @Nullable
  @Override
  public Color getBgColor(int i, Editor editor) {
    return null;
  }

  @Override
  public List<AnAction> getPopupActions(int i, Editor editor) {
    return null;
  }

  @Override
  public void gutterClosed() {

  }
}
