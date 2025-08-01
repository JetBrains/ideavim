/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.VimApi

/**
 * Scope for working with modal input in IdeaVim.
 *
 * This scope provides methods for creating and managing modal input dialogs,
 * which can be used to get user input in a Vim-like way.
 *
 * The ModalInput interface supports:
 * - Single character input with [inputChar]
 * - String input with [inputString]
 * - Repeating input operations with [repeat] and [repeatWhile]
 * - Updating the input prompt with [updateLabel]
 * - Closing the current input dialog with [closeCurrentInput]
 */
@VimApiDsl
interface ModalInput {
  /**
   * Updates the label of the modal input dialog during input processing.
   *
   * This method allows you to dynamically modify the label shown to the user based on the current state.
   *
   * Example usage:
   * ```kotlin
   * modalInput()
   *   .updateLabel { currentLabel ->
   *     "$currentLabel - Updated"
   *   }
   *   .inputChar("Enter character:") { char ->
   *     // Process the character
   *   }
   * ```
   *
   * @param block A function that takes the current label and returns a new label
   * @return This ModalInput instance for method chaining
   */
  fun updateLabel(block: (String) -> String): ModalInput

  /**
   * Repeats the input operation as long as the specified condition is true.
   *
   * This method allows you to collect multiple inputs from the user until a certain condition is met.
   * The condition is evaluated before each input operation.
   *
   * Example usage:
   * ```kotlin
   * var inputCount = 0
   *
   * modalInput()
   *   .repeatWhile {
   *     inputCount < 3  // Continue until we've received 3 inputs
   *   }
   *   .inputChar("Enter character:") { char ->
   *     inputCount++
   *     // Process the character
   *   }
   * ```
   *
   * @param condition A function that returns true if the input operation should be repeated
   * @return This ModalInput instance for method chaining
   */
  fun repeatWhile(condition: () -> Boolean): ModalInput

  /**
   * Repeats the input operation a specified number of times.
   *
   * This method allows you to collect a fixed number of inputs from the user.
   *
   * Example usage:
   * ```kotlin
   * modalInput()
   *   .repeat(3)  // Get 3 characters from the user
   *   .inputChar("Enter character:") { char ->
   *     // Process each character as it's entered
   *     // This handler will be called 3 times
   *   }
   * ```
   *
   * @param count The number of times to repeat the input operation
   * @return This ModalInput instance for method chaining
   */
  fun repeat(count: Int): ModalInput

  /**
   * Creates a modal input dialog for collecting a string from the user.
   *
   * This method displays a dialog with the specified label and waits for the user to enter text.
   * The handler is executed after the user presses ENTER, receiving the entered string as a parameter.
   *
   * Example usage:
   * ```kotlin
   * modalInput()
   *   .inputString("Enter string:") { enteredString ->
   *     // Process the entered string
   *     println("User entered: $enteredString")
   *   }
   * ```
   *
   * This can be combined with other methods:
   *
   * ```kotlin
   * vimApi.modalInput()
   *   .repeat(2)  // Get two strings from the user
   *   .inputString("Enter value:") { value ->
   *     // Process each string as it's entered
   *   }
   * ```
   *
   * @param label The label to display in the dialog
   * @param handler A function that will be called when the user enters input and presses ENTER
   */
  fun inputString(label: String, handler: VimApi.(String) -> Unit)

  /**
   * Creates a modal input dialog for collecting a single character from the user.
   *
   * This method displays a dialog with the specified label and waits for the user to press a key.
   * The handler is executed immediately after the user presses any key, receiving the entered character as a parameter.
   * Unlike [inputString], this method doesn't require the user to press ENTER.
   *
   * Example usage:
   * ```kotlin
   * vimApi.modalInput()
   *   .inputChar("Press a key:") { char ->
   *     // Process the entered character
   *     when(char) {
   *       'y', 'Y' -> println("You confirmed")
   *       'n', 'N' -> println("You declined")
   *       else -> println("Invalid option")
   *     }
   *   }
   * ```
   *
   * This can be combined with other methods:
   *
   * ```kotlin
   * vimApi.modalInput()
   *   .repeatWhile { /* condition */ }
   *   .inputChar("Enter character:") { char ->
   *     // Process each character as it's entered
   *   }
   * ```
   *
   * @param label The label to display in the dialog
   * @param handler A function that will be called when the user enters a character
   */
  fun inputChar(label: String, handler: VimApi.(Char) -> Unit)

  /**
   * Closes the current modal input dialog, if one is active.
   *
   * Example usage:
   * ```kotlin
   * modalInput().closeCurrentInput(refocusEditor = false)
   * ```
   *
   * @param refocusEditor Whether to refocus the editor after closing the dialog (default: true)
   * @return True if a dialog was closed, false if there was no active dialog
   */
  fun closeCurrentInput(refocusEditor: Boolean = true): Boolean
}
