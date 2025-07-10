/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Interface for interacting with the Vim output panel.
 * 
 * The output panel is used to display text output from Vim commands and operations.
 * This scope provides methods to manipulate the content and appearance of the output panel.
 */
interface OutputPanelScope {
  /**
   * The text displayed in the output panel.
   */
  val text: String

  /**
   * The label text displayed at the bottom of the output panel.
   * 
   * This is used for status information like "-- MORE --" to indicate
   * that there is more content to scroll through.
   */
  val label: String

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
   * Sets the label text at the bottom of the output panel.
   * 
   * @param label The new label text to display.
   */
  suspend fun setLabel(label: String)

  /**
   * Clears all text from the output panel.
   */
  suspend fun clearText()
}
