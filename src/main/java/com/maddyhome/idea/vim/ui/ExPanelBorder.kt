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

internal class ExPanelBorder : SideBorder(JBColor.border(), TOP) {

  override fun getBorderInsets(component: Component?): Insets {
    return JBInsets(getThickness() + 2, 0, 2, 2)
  }

  override fun getBorderInsets(component: Component?, insets: Insets): Insets {
    insets.top = getThickness() + 2
    insets.left = 0
    insets.bottom = 2
    insets.right = 2
    return insets
  }
}
