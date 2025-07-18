/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import java.awt.Graphics
import java.awt.Shape
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.text.Element
import javax.swing.text.Position
import javax.swing.text.ViewFactory
import kotlin.math.max
import kotlin.math.min

/**
 * A specialisation of [InlineCompositeView] that can scroll the content into view
 *
 * Whenever this view needs to perform some kind of layout, such as converting model index to view coordinates, it
 * adjusts the allocation to account for the scroll position. It is used by [ExEditorKit] for a styled document's
 * section element type.
 */
class ScrollingInlineCompositeView(elem: Element) : InlineCompositeView(elem) {
  override fun modelToView(pos: Int, a: Shape?, b: Position.Bias?): Shape? {
    return super.modelToView(pos, adjustAllocation(a), b)
  }

  override fun viewToModel(x: Float, y: Float, a: Shape?, bias: Array<out Position.Bias?>?): Int {
    return super.viewToModel(x, y, adjustAllocation(a), bias)
  }

  override fun insertUpdate(changes: DocumentEvent?, a: Shape?, f: ViewFactory?) {
    super.insertUpdate(changes, adjustAllocation(a), f)
  }

  override fun removeUpdate(changes: DocumentEvent?, a: Shape?, f: ViewFactory?) {
    super.removeUpdate(changes, adjustAllocation(a), f)
  }

  override fun paint(g: Graphics, allocation: Shape) {
    super.paint(g, adjustAllocation(allocation)!!)
  }

  private fun adjustAllocation(a: Shape?): Shape? {
    return a?.bounds?.also { bounds ->
      (container as? JTextField)?.let { c ->
        val visibility = c.horizontalVisibility

        val contentWidth = getPreferredSpan(X_AXIS).toInt()
        val containerWidth = bounds.width
        val maximum = max(contentWidth, containerWidth)
        val extent = min(maximum, containerWidth - 1)
        val value = if (visibility.value + extent > maximum) maximum - extent else visibility.value
        visibility.setRangeProperties(value, extent, visibility.minimum, maximum, false)

        if (contentWidth >= containerWidth) {
          bounds.width = contentWidth
          bounds.x -= visibility.value
        } else {
          // TODO: This is where we would handle right or centred aligned text, but we don't care about that
        }
      }
    }
  }
}
