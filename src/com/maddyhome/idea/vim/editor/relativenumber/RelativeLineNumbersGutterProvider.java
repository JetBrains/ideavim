package com.maddyhome.idea.vim.editor.relativenumber;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorFontType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * Gutter provider that sets the line number relative to the current caret position.
 */
public class RelativeLineNumbersGutterProvider implements TextAnnotationGutterProvider {

  private boolean enabled = false;

  @Nullable
  @Override
  public String getLineText(int i, Editor editor) {
    if (enabled) {
      int currentLine = editor.getCaretModel().getLogicalPosition().line;
      return "" + Math.abs(currentLine - i);
    }
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

  public void enabled() { enabled = true; }
  public void disabled() { enabled = false; }
}
