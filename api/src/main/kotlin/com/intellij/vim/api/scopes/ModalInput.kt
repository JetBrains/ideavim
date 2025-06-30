/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Scope for working with modal input in IdeaVim.
 * 
 * This scope provides methods for creating and managing modal input dialogs,
 * which can be used to get user input in a Vim-like way.
 */
@VimPluginDsl
interface ModalInput {
  fun updateLabel(block: (String) -> String): ModalInput

  fun repeatWhile(condition: () -> Boolean): ModalInput

  fun repeat(count: Int): ModalInput

  /**
   * Creates a modal input dialog with the given label and handler. Handler will be executed after the user presses ENTER.
   *
   * @param label The label to display in the dialog
   * @param handler A function that will be called when the user enters input (after pressing ENTER)
   * @return True if the input was processed successfully, false otherwise
   */
  fun inputString(label: String, handler: VimScope.(String) -> Unit)

  /**
   * Creates a modal input dialog with the given label and handler.
   *
   * @param label The label to display in the dialog
   * @param handler A function that will be called when the user enters input (after pressing ENTER)
   * @return True if the input was processed successfully, false otherwise
   */
  fun inputChar(label: String, handler: VimScope.(Char) -> Unit)

  /**
   * Closes the current modal input dialog, if any.
   *
   * @param refocusEditor Whether to refocus the editor after closing the dialog
   * @return True if a dialog was closed, false otherwise
   */
  fun closeCurrentInput(refocusEditor: Boolean = true): Boolean
}