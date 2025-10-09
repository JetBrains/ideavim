/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import javax.swing.text.AttributeSet
import javax.swing.text.Element
import javax.swing.text.LabelView
import javax.swing.text.Segment
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.View

/**
 * A simple View to draw a prompt character
 *
 * Used while entering a digraph (`?`) or literal (`^`). Note that this isn't a typical [View] since it doesn't
 * correspond to a character in a [Document] or [Element], it is added to the parent to show a character that otherwise
 * doesn't exist.
 */
class ExFakePromptView(private val prompt: Char, elem: Element, private val offset: Int) : LabelView(elem) {
  override fun getText(p0: Int, p1: Int): Segment {
    return Segment(charArrayOf(prompt), 0, (p1 - p0).coerceAtMost(1))
  }

  override fun getStartOffset() = offset
  override fun getEndOffset() = offset + 1  // End exclusive

  override fun getAttributes(): AttributeSet {
    val attributes = SimpleAttributeSet()
    attributes.resolveParent = super.getAttributes()
    val specialKeyStyle = (element.document as ExDocument).getStyle(ExDocument.SPECIAL_KEY_STYLE_NAME)
    attributes.addAttributes(specialKeyStyle)
    return attributes
  }
}
