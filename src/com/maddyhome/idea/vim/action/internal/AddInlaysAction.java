/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.internal;

import com.intellij.ide.ui.AntialiasingType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.Random;

public class AddInlaysAction extends AnAction {
  private static Random random = new Random();

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Editor editor = getEditor(dataContext);
    if (editor == null) return;

    InlayModel inlayModel = editor.getInlayModel();

    Document document = editor.getDocument();
    int lineCount = document.getLineCount();
    for (int i = random.nextInt(10); i < lineCount; ) {

      int offset = document.getLineStartOffset(i);

      // Mostly above
      boolean above = random.nextInt(10) > 3;

      // Mostly do one, but occasionally throw in a bunch
      int count = random.nextInt(10) > 7 ? random.nextInt(5) : 1;
      for (int j = 0; j < count; j++) {

        float factor = Math.max(1.75f * random.nextFloat(), 0.9f);
        String text = String.format("---------- %s line %d ----------", above ? "above" : "below", i + 1);

        inlayModel.addBlockElement(offset, true, above, 0, new MyBlockRenderer(factor, text));
      }

      // Every 10 lines +/- 3 lines
      i += 10 + (random.nextInt(6) - 3);
    }
  }

  protected Editor getEditor(@NotNull DataContext dataContext) {
    return CommonDataKeys.EDITOR.getData(dataContext);
  }

  private static class MyBlockRenderer implements EditorCustomElementRenderer {

    private static Key<MyFontMetrics> HINT_FONT_METRICS = Key.create("DummyInlayFontMetrics");
    private float factor;
    private String text;

    MyBlockRenderer(float factor, String text) {
      this.factor = factor;
      this.text = text;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
      Editor editor = inlay.getEditor();
      FontMetrics fontMetrics = getFontMetrics(editor).metrics;
      return doCalcWidth(text, fontMetrics);
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
      Editor editor = inlay.getEditor();
      FontMetrics fontMetrics = getFontMetrics(editor).metrics;
      return fontMetrics.getHeight();
    }

    @Override
    public void paint(@NotNull Inlay inlay,
                      @NotNull Graphics g,
                      @NotNull Rectangle targetRegion,
                      @NotNull TextAttributes textAttributes) {
      Editor editor = inlay.getEditor();
      FontMetrics fontMetrics = getFontMetrics(editor).metrics;
      LineMetrics lineMetrics = fontMetrics.getLineMetrics(text, g);

      g.setColor(JBColor.GRAY);
      g.setFont(fontMetrics.getFont());
      g.drawString(text, 0, targetRegion.y + (int)(lineMetrics.getHeight() - lineMetrics.getDescent()));
      g.setColor(JBColor.LIGHT_GRAY);
      g.drawRect(targetRegion.x, targetRegion.y, targetRegion.width, targetRegion.height);
    }

    private MyFontMetrics getFontMetrics(Editor editor) {
      String familyName = UIManager.getFont("Label.font").getFamily();
      int size = (int)(Math.max(1, editor.getColorsScheme().getEditorFontSize() - 1) * factor);
      MyFontMetrics metrics = editor.getUserData(HINT_FONT_METRICS);
      if (metrics != null && !metrics.isActual(editor, familyName, size)) {
        metrics = null;
      }
      if (metrics == null) {
        metrics = new MyFontMetrics(editor, familyName, size);
        editor.putUserData(HINT_FONT_METRICS, metrics);
      }
      return metrics;
    }

    private int doCalcWidth(String text, FontMetrics fontMetrics) {
      return (text == null) ? 0 : fontMetrics.stringWidth(text);
    }


    protected class MyFontMetrics {
      private FontMetrics metrics;

      MyFontMetrics(Editor editor, String familyName, int size) {
        Font font = UIUtil.getFontWithFallback(familyName, Font.PLAIN, size);
        FontRenderContext context = getCurrentContext(editor);
        metrics = FontInfo.getFontMetrics(font, context);
        // We assume this will be a better approximation to a real line height for a given font
      }

      public boolean isActual(Editor editor, String familyName, int size) {
        Font font = metrics.getFont();
        if (!familyName.equals(font.getFamily()) || size != font.getSize()) return false;
        FontRenderContext currentContext = getCurrentContext(editor);
        return currentContext.equals(metrics.getFontRenderContext());
      }

      private FontRenderContext getCurrentContext(Editor editor) {
        FontRenderContext editorContext = FontInfo.getFontRenderContext(editor.getContentComponent());
        return new FontRenderContext(editorContext.getTransform(),
                                     AntialiasingType.getKeyForCurrentScope(false), editor instanceof EditorImpl
                                                                                    ? ((EditorImpl)editor).myFractionalMetricsHintValue
                                                                                    : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
      }
    }
  }
}
