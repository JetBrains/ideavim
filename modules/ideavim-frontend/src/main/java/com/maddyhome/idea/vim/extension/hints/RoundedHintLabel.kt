/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

/**
 * A rounded hint label component with a blue color scheme.
 * Colors are based on the Islands theme blue palette.
 */
class RoundedHintLabel(private val hintText: String) : JComponent() {

  private val arc = JBUI.scale(6)
  private val padX = JBUI.scale(4)
  private val padY = JBUI.scale(2)

  init {
    isOpaque = false
    font = JBUI.Fonts.smallFont().deriveFont(Font.BOLD)
  }

  override fun getPreferredSize(): Dimension {
    val fm = getFontMetrics(font)
    val strokeExtra = JBUI.scale(1) * 2
    val w = fm.stringWidth(hintText) + padX * 2 + strokeExtra
    val h = fm.height + padY * 2 + strokeExtra
    return Dimension(w, h)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

      val w = width
      val h = height

      g2.color = BACKGROUND
      g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)

      g2.color = BORDER
      g2.stroke = BasicStroke(JBUI.scale(1).toFloat())
      g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)

      g2.font = font
      g2.color = FOREGROUND
      val fm = g2.fontMetrics
      val textX = padX
      val textY = (h - fm.height) / 2 + fm.ascent
      g2.drawString(hintText, textX, textY)
    } finally {
      g2.dispose()
    }
  }

  companion object {
    // Blue colors from Islands theme
    // Light: blue-20, blue-40, blue-100
    // Dark: blue-130, blue-110, blue-70
    private val BACKGROUND: Color = JBColor(0xE3EBFE, 0x233558)
    private val BORDER: Color = JBColor(0xBDD3FF, 0x2E4D89)
    private val FOREGROUND: Color = JBColor(0x2F5EB9, 0x71A1FE)
  }
}
