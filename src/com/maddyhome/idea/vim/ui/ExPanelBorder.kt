package com.maddyhome.idea.vim.ui

import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.util.ui.JBInsets

import java.awt.*

class ExPanelBorder internal constructor() : SideBorder(JBColor.border(), SideBorder.TOP) {

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
