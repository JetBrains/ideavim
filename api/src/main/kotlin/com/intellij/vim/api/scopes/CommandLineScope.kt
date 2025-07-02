/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Interface for interacting with the Vim command line.
 * 
 * The command line is used for entering Ex commands, search patterns, and other input.
 * This scope provides methods to create, manipulate, and interact with the command line.
 */
@VimPluginDsl
interface CommandLineScope {
  /**
   * The text currently displayed in the command line.
   */
  val text: String
  
  /**
   * The current position of the caret in the command line.
   */
  val caretPosition: Int

  /**
   * True if the command line is currently active, false otherwise.
   */
  val isActive: Boolean

  /**
   * Sets the text content of the command line.
   *
   * This replaces any existing text in the command line with the provided text.
   *
   * @param text The new text to display in the command line.
   */
  fun setText(text: String)

  /**
   * Inserts text at the specified position in the command line.
   *
   * @param offset The position at which to insert the text.
   * @param text The text to insert.
   */
  fun insertText(offset: Int, text: String)

  /**
   * Sets the caret position in the command line.
   *
   * @param position The new position for the caret.
   */
  fun setCaretPosition(position: Int)

  /**
   * Reads input from the command line and processes it with the provided function.
   *
   * @param prompt The prompt to display at the beginning of the command line.
   * @param finishOn The character that, when entered, will finish the input process. If null, only Enter will finish.
   * @param callback A function that will be called with the entered text when input is complete.
   */
  fun input(prompt: String, finishOn: Char? = null, callback: VimScope.(String) -> Unit)

  /**
   * Closes the command line.
   *
   * @param refocusEditor Whether to refocus the editor after closing the command line.
   * @return True if the command line was closed, false if it was not active.
   */
  fun close(refocusEditor: Boolean = true): Boolean
}