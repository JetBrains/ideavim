/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Scope that provides functions for interacting with the Vim output panel.
 */
@VimApiDsl
interface OutputPanelScope {
  /**
   * Sets the text content of the output panel.
   *
   * This replaces any existing text in the panel with the provided text.
   *
   * @param text The new text to display in the output panel.
   */
  suspend fun setText(text: String)

  /**
   * Appends text to the existing content of the output panel.
   *
   * @param text The text to append to the current content.
   * @param startNewLine Whether to start the appended text on a new line.
   *                     If true and there is an existing text, a newline character
   *                     will be inserted before the appended text.
   *                     Defaults to false.
   */
  suspend fun appendText(text: String, startNewLine: Boolean = false)

  /**
   * Clears all text from the output panel.
   */
  suspend fun clearText()
}
