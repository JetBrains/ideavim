/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ui

import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.util.ui.JBInsets
import java.awt.Component
import java.awt.Insets

class ExPanelBorder internal constructor() : SideBorder(JBColor.border(), TOP) {

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
