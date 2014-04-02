package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author vlan
 */
public class UiHelper {
  private UiHelper() {
  }


  @NotNull
  public static Font getEditorFont() {
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    return new Font(scheme.getEditorFontName(), Font.PLAIN, scheme.getEditorFontSize());
  }
}
