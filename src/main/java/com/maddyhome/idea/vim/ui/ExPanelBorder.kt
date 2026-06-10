/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.util.ui.JBInsets
import java.awt.Component
import java.awt.Insets

/**
 * Creates the border for the ex-entry panel and output panel. It sets up insets and only paints on the top edge
 */
internal class ExPanelBorder : SideBorder(JBColor.border(), TOP) {
  companion object {
    const val VERTICAL_INSET = 3
    const val HORIZONTAL_INSET = 5
  }

  override fun getBorderInsets(component: Component?): Insets {
    return JBInsets(getThickness() + VERTICAL_INSET, HORIZONTAL_INSET, VERTICAL_INSET, HORIZONTAL_INSET)
  }

  override fun getBorderInsets(component: Component?, insets: Insets): Insets {
    insets.top = getThickness() + VERTICAL_INSET
    insets.left = HORIZONTAL_INSET
    insets.bottom = VERTICAL_INSET
    insets.right = HORIZONTAL_INSET
    return insets
  }
}
