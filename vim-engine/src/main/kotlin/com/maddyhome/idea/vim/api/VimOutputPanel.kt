/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimOutputPanel {
  /**
   * The current text displayed in the output panel.
   * The actual text may be different (if we called the [addText] method and did not call [show] afterward)
   */
  val text: String

  /**
   * Appends the specified text to the existing content of the output panel.
   * If 'isNewLine' is true, the text will begin on a new line.
   *
   * Note: The full text content is not updated in the display until [show] is invoked.
   *
   * @param text The text to append.
   * @param isNewLine Whether to start the appended text on a new line. Defaults to true.
   */
  fun addText(text: String, isNewLine: Boolean = true)

  /**
   * This method shows the text output or updates the output text if the panel was already shown
   */
  fun show()

  /**
   * Disposes or hides the output panel, depending on its implementation.
   * This may free any associated resources.
   */
  fun close()
}