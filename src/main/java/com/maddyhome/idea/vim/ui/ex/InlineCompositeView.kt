package com.maddyhome.idea.vim.ui.ex

import fleet.util.max
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.Shape
import javax.swing.text.CompositeView
import javax.swing.text.Element
import javax.swing.text.View

/**
 * A simple view that maintains child views in a single line
 *
 * An implementation of [CompositeView] that does no layout other than positioning child views horizontally in a single
 * line, all in their preferred size. Used by [ExEditorKit] for a styled document's section and paragraph element types.
 * A section contains a single paragraph, but a paragraph can contain multiple styled leaves.
 */
open class InlineCompositeView(elem: Element) : CompositeView(elem) {
  override fun isBefore(x: Int, y: Int, alloc: Rectangle) = x < alloc.x
  override fun isAfter(x: Int, y: Int, alloc: Rectangle) = x > alloc.x + alloc.width

  override fun getViewAtPoint(x: Int, y: Int, alloc: Rectangle): View {
    var span = 0f
    for (n in 0 until viewCount) {
      val view = getView(n)
      span += view.getPreferredSpan(X_AXIS)
      if (span >= x) {
        val index = (n - 1).coerceAtLeast(0)
        childAllocation(index, alloc)
        return getView(index)
      }
    }
    childAllocation(viewCount - 1, alloc)
    return getView(viewCount - 1)
  }

  override fun getViewIndexAtPosition(pos: Int): Int {
    // We might have multiple views for the current element, and the default implementation of CompositeView returns the
    // index of the child element at the given offset. We need to find the view that holds the given offset. This is
    // used to e.g. calculate the position of the caret
    for (n in 0 until viewCount) {
      val view = getView(n)
      if (pos >= view.startOffset && pos < view.endOffset) {  // End offset is exclusive
        return n
      }
    }
    return -1
  }

  override fun childAllocation(index: Int, a: Rectangle?) {
    a?.let {
      var lastSpan = 0
      for (n in 0..index) {
        it.x += lastSpan
        val view = getView(n)
        lastSpan = view.getPreferredSpan(X_AXIS).toInt()
        it.width = lastSpan
      }
    }
  }

  override fun getPreferredSpan(axis: Int): Float {
    return when (axis) {
      X_AXIS -> {
        var span = 0f
        for (n in 0 until viewCount) {
          val view = getView(n)
          span += view.getPreferredSpan(axis)
        }
        span
      }
      else /* Y_AXIS */ -> {
        var span = 0f
        for (n in 0 until viewCount) {
          val view = getView(n)
          val preferred = view.getPreferredSpan(axis)
          span = max(span, preferred)
        }
        span
      }
    }
  }

  override fun paint(g: Graphics, allocation: Shape) {
    val clipBounds = g.clipBounds
    val rect = Rectangle(allocation.bounds)
    for (n in 0 until viewCount) {
      val view = getView(n)
      val span = view.getPreferredSpan(X_AXIS)
      rect.width = span.toInt()
      if (rect.intersects(clipBounds)) {
        view.paint(g, rect)
      }
      rect.x += span.toInt()
    }
  }
}
