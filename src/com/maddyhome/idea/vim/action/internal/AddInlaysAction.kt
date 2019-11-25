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
package com.maddyhome.idea.vim.action.internal

import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.util.*
import javax.swing.UIManager

class AddInlaysAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val dataContext = e.dataContext
    val editor = getEditor(dataContext) ?: return
    val inlayModel = editor.inlayModel
    val document = editor.document
    val lineCount = document.lineCount
    var i = random.nextInt(10)
    while (i < lineCount) {
      val offset = document.getLineStartOffset(i)
      // Mostly above
      val above = random.nextInt(10) > 3
      // Mostly do one, but occasionally throw in a bunch
      val count = if (random.nextInt(10) > 7) random.nextInt(5) else 1
      for (j in 0 until count) {
        val factor = (1.75f * random.nextFloat()).coerceAtLeast(0.9f)
        val text = String.format("---------- %s line %d ----------", if (above) "above" else "below", i + 1)
        inlayModel.addBlockElement(offset, true, above, 0, MyBlockRenderer(factor, text))
      }
      // Every 10 lines +/- 3 lines
      i += 10 + (random.nextInt(6) - 3)
    }
  }

  private fun getEditor(dataContext: DataContext): Editor? {
    return CommonDataKeys.EDITOR.getData(dataContext)
  }

  private class MyBlockRenderer(private val factor: Float, private val text: String) : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
      val editor = inlay.editor
      val fontMetrics = getFontMetrics(editor).metrics
      return doCalcWidth(text, fontMetrics)
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
      val editor = inlay.editor
      val fontMetrics = getFontMetrics(editor).metrics
      return fontMetrics.height
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
      val editor = inlay.editor
      val fontMetrics = getFontMetrics(editor).metrics
      val lineMetrics = fontMetrics.getLineMetrics(text, g)
      g.color = JBColor.GRAY
      g.font = fontMetrics.font
      g.drawString(text, 0, targetRegion.y + (lineMetrics.height - lineMetrics.descent).toInt())
      g.color = JBColor.LIGHT_GRAY
      g.drawRect(targetRegion.x, targetRegion.y, targetRegion.width, targetRegion.height)
    }

    private fun getFontMetrics(editor: Editor): MyFontMetrics {
      val familyName = UIManager.getFont("Label.font").family
      val size = (Math.max(1, editor.colorsScheme.editorFontSize - 1) * factor).toInt()
      var metrics = editor.getUserData(HINT_FONT_METRICS)
      if (metrics != null && !metrics.isActual(editor, familyName, size)) {
        metrics = null
      }
      if (metrics == null) {
        metrics = MyFontMetrics(editor, familyName, size)
        editor.putUserData(HINT_FONT_METRICS, metrics)
      }
      return metrics
    }

    private fun doCalcWidth(text: String?, fontMetrics: FontMetrics): Int {
      return if (text == null) 0 else fontMetrics.stringWidth(text)
    }

    private inner class MyFontMetrics internal constructor(editor: Editor, familyName: String?, size: Int) {
      val metrics: FontMetrics
      fun isActual(editor: Editor, familyName: String, size: Int): Boolean {
        val font = metrics.font
        if (familyName != font.family || size != font.size) return false
        val currentContext = getCurrentContext(editor)
        return currentContext.equals(metrics.fontRenderContext)
      }

      private fun getCurrentContext(editor: Editor): FontRenderContext {
        val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
        return FontRenderContext(editorContext.transform, AntialiasingType.getKeyForCurrentScope(false),
          if (editor is EditorImpl) editor.myFractionalMetricsHintValue else RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
      }

      init {
        val font: Font = UIUtil.getFontWithFallback(familyName, Font.PLAIN, size)
        val context = getCurrentContext(editor)
        metrics = FontInfo.getFontMetrics(font, context)
        // We assume this will be a better approximation to a real line height for a given font
      }
    }

    companion object {
      private val HINT_FONT_METRICS = Key.create<MyFontMetrics>("DummyInlayFontMetrics")
    }

  }

  companion object {
    private val random = Random()
  }
}
