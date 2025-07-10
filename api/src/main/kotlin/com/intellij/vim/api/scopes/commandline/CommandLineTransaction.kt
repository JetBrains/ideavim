/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.commandline

interface CommandLineTransaction {
  /**
   * Sets the text content of the command line.
   *
   * This replaces any existing text in the command line with the provided text.
   *
   * @param text The new text to display in the command line.
   */
  suspend fun setText(text: String)

  /**
   * Inserts text at the specified position in the command line.
   *
   * @param offset The position at which to insert the text.
   * @param text The text to insert.
   */
  suspend fun insertText(offset: Int, text: String)

  /**
   * Sets the caret position in the command line.
   *
   * @param position The new position for the caret.
   */
  suspend fun setCaretPosition(position: Int)

  /**
   * Closes the command line.
   *
   * @param refocusEditor Whether to refocus the editor after closing the command line.
   * @return True if the command line was closed, false if it was not active.
   */
  suspend fun close(refocusEditor: Boolean = true): Boolean
}