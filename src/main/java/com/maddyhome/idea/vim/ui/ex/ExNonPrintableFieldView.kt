/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.maddyhome.idea.vim.helper.EngineStringHelper
import javax.swing.text.Element
import javax.swing.text.LabelView
import javax.swing.text.Segment
import javax.swing.text.StyledDocument

/**
 * View to print non-printable characters in the Ex command line
 *
 * When asked for the text to paint, this view will return a printable version, as formatted by Vim. E.g., when trying
 * to print `\u0080` or LF/`\u000C`, normally Swing would draw it as an empty space. This view will tell the underlying
 * [LabelView] to print `<80>` or `^L` instead. It simply provides different text to the painter, and other issues such
 * as size management, caret position and painting are handled by the superclass.
 *
 * Note that the given element might contain more than one character. The owning [StyledDocument] splits text based on
 * attributes, and our [ExDocument] adds a semantic attribute to non-printable characters. If multiple non-printable
 * characters are side by side, they are grouped into the same leaf element.
 */
class ExNonPrintableFieldView(elem: Element) : LabelView(elem) {

  /**
   * Get the text used for measuring and painting
   *
   * Typically, this would return the text from the document, which in our case will be non-printable characters. This
   * view converts these to Vim-formatted printable characters and returns that text instead.
   */
  override fun getText(p0: Int, p1: Int): Segment {
    val actualText = super.getText(p0, p1)
    val renderedText = buildString {
      actualText.first()
      while (actualText.current() != Segment.DONE) {
        append(EngineStringHelper.toPrintableCharacter(actualText.current()))
        actualText.next()
      }
    }

    return Segment(renderedText.toCharArray(), 0, renderedText.length)
  }

  override fun getBreakWeight(axis: Int, pos: Float, len: Float) = BadBreakWeight
  override fun breakView(axis: Int, p0: Int, pos: Float, len: Float) = this
}
