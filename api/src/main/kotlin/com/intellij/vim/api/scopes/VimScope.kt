/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.Mode
import com.intellij.vim.api.Path
import com.intellij.vim.api.scopes.commandline.CommandLineScope
import com.intellij.vim.api.scopes.editor.EditorScope
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Scope that provides vim functions and is an entry point for the other scopes.
 */
@VimApiDsl
interface VimScope {
  /**
   * Represents the current mode in Vim.
   *
   * Example usage:
   *
   * **Getting the Current Mode**
   * ```kotlin
   * val currentMode = mode
   * println("Current Vim Mode: $currentMode")
   * ```
   */
  val mode: Mode

  /**
   * Retrieves a variable of the specified type and name.
   * Use the extension function `getVariable<String>("name")`
   */
  fun <T : Any> getVariable(name: String, type: KType): T?

  /**
   * Sets a variable with the specified name and value.
   * Use the extension function `setVariable<String>("name", 1)`
   *
   * In Vim, this is equivalent to `let varname = value`.
   */
  fun setVariable(name: String, value: Any, type: KType)

  /**
   * Exports a function that can be used as an operator function in Vim.
   *
   * In Vim, operator functions are used with the `g@` operator to create custom operators.
   *
   * Example usage:
   * ```kotlin
   * exportOperatorFunction("MyOperator") {
   *     editor {
   *         // Perform operations on the selected text
   *         true // Return success
   *     }
   * }
   * ```
   *
   * @param name The name to register the function under
   * @param function The function to execute when the operator is invoked
   */
  fun exportOperatorFunction(name: String, function: suspend VimScope.() -> Boolean)

  /**
   * Sets the current operator function to use with the `g@` operator.
   *
   * In Vim, this is equivalent to setting the 'operatorfunc' option.
   *
   * @param name The name of the previously exported operator function
   */
  fun setOperatorFunction(name: String)

  /**
   * Executes normal mode commands as if they were typed.
   *
   * In Vim, this is equivalent to the `:normal` command.
   *
   * Example usage:
   * ```kotlin
   * normal("gg")  // Go to the first line
   * normal("dw")  // Delete word
   * ```
   *
   * @param command The normal mode command string to execute
   */
  fun normal(command: String)

  /**
   * Executes a block of code in the context of the currently focused editor.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *     read {
   *       // executed under read lock
   *     }
   * }
   * ```
   *
   * @param block The code block to execute within editor scope
   * @return The result of the block execution
   */
  fun <T> editor(block: EditorScope.() -> T): T

  /**
   * Executes a block of code for each editor.
   *
   * This function allows performing operations on all available editors.
   *
   * Example usage:
   * ```kotlin
   * forEachEditor {
   *     // Perform some operation on each editor
   * }
   * ```
   *
   * @param block The code block to execute for each editor
   * @return A list containing the results of executing the block on each editor
   */
  fun <T> forEachEditor(block: EditorScope.() -> T): List<T>

  /**
   * Provides access to key mapping functionality.
   *
   * Example usage:
   * ```kotlin
   * mappings {
   *    nmap("jk", "<Esc>")
   * }
   * ```
   *
   * @param block The code block to execute within the mapping scope
   */
  fun mappings(block: MappingScope.() -> Unit)

  /**
   * Provides access to event listener functionality.
   *
   * Example usage:
   * ```kotlin
   * listeners {
   *     // Register a listener for mode changes
   *     onModeChange { oldMode ->
   *         println("Mode changed from $oldMode")
   *     }
   * }
   * ```
   *
   * @param block The code block to execute within the listeners scope
   */
  fun listeners(block: ListenersScope.() -> Unit)

  /**
   * Provides access to Vim's output panel functionality.
   *
   * Example usage:
   * ```kotlin
   * outputPanel {
   *     // Print a message to the output panel
   *     setText("Hello from IdeaVim plugin!")
   * }
   * ```
   *
   * @param block The code block to execute within the output panel scope
   */
  fun outputPanel(block: OutputPanelScope.() -> Unit)

  /**
   * Provides access to modal input functionality.
   *
   * Example usage:
   * ```kotlin
   * modalInput()
   *  .inputChar(label) { char ->
   *    // get char that user entered
   *  }
   * ```
   *
   * @return A ModalInput instance that can be used to request user input
   */
  fun modalInput(): ModalInput

  /**
   * Provides access to Vim's command line functionality.
   *
   * Example usage:
   * ```kotlin
   * commandLine {
   *    // get current command line text
   *    read {
   *      // executed under read lock
   *      text
   *    }
   * }
   * ```
   *
   * @param block The code block to execute with command line scope
   */
  fun commandLine(block: CommandLineScope.() -> Unit)

  /**
   * Provides access to Vim's options functionality.
   *
   * Example usage:
   * ```kotlin
   * option {
   *     // Get option value
   *     get<Boolean>("number")
   *
   *     // Set option value
   *     set<Boolean>("number", true)
   * }
   * ```
   *
   * @param block The code block to execute within the option scope
   */
  fun option(block: OptionScope.() -> Unit)

  /**
   * Provides access to Vim's digraph functionality.
   *
   * Example usage:
   * ```kotlin
   * digraph {
   *     // Add a new digraph
   *     add("a:", 'ä')
   * }
   * ```
   *
   * @param block The code block to execute within the digraph scope
   */
  fun digraph(block: DigraphScope.() -> Unit)

  /**
   * Gets the number of tabs in the current window.
   */
  val tabCount: Int

  /**
   * The index of the current tab or null if there is no tab selected or no tabs are open
   */
  val currentTabIndex: Int?

  /**
   * Removes a tab at the specified index and selects another tab.
   *
   * @param indexToDelete The index of the tab to delete
   * @param indexToSelect The index of the tab to select after deletion
   */
  fun removeTabAt(indexToDelete: Int, indexToSelect: Int)

  /**
   * Moves the current tab to the specified index.
   *
   * @param index The index to move the current tab to
   * @throws IllegalStateException if there is no tab selected or no tabs are open
   */
  fun moveCurrentTabToIndex(index: Int)

  /**
   * Closes all tabs except the current one.
   *
   * @throws IllegalStateException if there is no tab selected
   */
  fun closeAllExceptCurrentTab()

  /**
   * Checks if a pattern matches a text.
   *
   * @param pattern The regular expression pattern to match
   * @param text The text to check against the pattern
   * @param ignoreCase Whether to ignore case when matching
   * @return True if the pattern matches the text, false otherwise
   */
  fun matches(pattern: String, text: String, ignoreCase: Boolean = false): Boolean

  /**
   * Finds all matches of a pattern in a text.
   *
   * @param text The text to search in
   * @param pattern The regular expression pattern to search for
   * @return A list of pairs representing the start and end offsets of each match
   */
  fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>>

  /**
   * Selects the next window in the editor.
   */
  fun selectNextWindow()

  /**
   * Selects the previous window in the editor.
   */
  fun selectPreviousWindow()

  /**
   * Selects a window by its index.
   *
   * @param index The index of the window to select (1-based).
   */
  fun selectWindow(index: Int)

  /**
   * Splits the current window vertically and optionally opens a file in the new window.
   *
   * @param filePath Path of the file to open in the new window. If null, the new window will show the same file.
   */
  fun splitWindowVertically(filePath: Path? = null)

  /**
   * Splits the current window horizontally and optionally opens a file in the new window.
   *
   * @param filePath Path of the file to open in the new window. If null, the new window will show the same file.
   */
  fun splitWindowHorizontally(filePath: Path? = null)

  /**
   * Closes all windows except the current one.
   */
  fun closeAllExceptCurrentWindow()

  /**
   * Closes the current window.
   */
  fun closeCurrentWindow()

  /**
   * Closes all windows in the editor.
   */
  fun closeAllWindows()

  /**
   * Parses and executes the given Vimscript string.
   *
   * @param script The Vimscript string to execute
   * @return The result of the execution, which can be Success or Error
   */
  fun execute(script: String): Boolean

  /**
   * Registers a new Vim command.
   *
   * Example usage:
   * ```
   * command("MyCommand") { cmd ->
   *     println("Command executed: $cmd")
   * }
   * ```
   *
   * @param command The name of the command to register, as entered by the user.
   * @param block The logic to execute when the command is invoked. Receives the command name
   *              entered by the user as a parameter.
   */
  fun command(command: String, block: VimScope.(String) -> Unit)

  /**
   * Gets keyed data from a Vim window.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  fun <T> getDataFromWindow(key: String): T?

  /**
   * Stores keyed user data in a Vim window.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  fun <T> putDataToWindow(key: String, data: T)

  /**
   * Gets data from buffer.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  fun <T> getDataFromBuffer(key: String): T?

  /**
   * Puts data to buffer.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  fun <T> putDataToBuffer(key: String, data: T)

  /**
   * Gets data from tab (group of windows).
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  fun <T> getDataFromTab(key: String): T?

  /**
   * Puts data to tab (group of windows).
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  fun <T> putDataToTab(key: String, data: T)

  /**
   * Gets data from window or puts it if it doesn't exist.
   *
   * @param key The key to retrieve or store data for
   * @param provider A function that provides the data if it doesn't exist
   * @return The existing data or the newly created data
   */
  fun <T> getOrPutWindowData(key: String, provider: () -> T): T =
    getDataFromWindow(key) ?: provider().also { putDataToWindow(key, it) }

  /**
   * Gets data from buffer or puts it if it doesn't exist.
   *
   * @param key The key to retrieve or store data for
   * @param provider A function that provides the data if it doesn't exist
   * @return The existing data or the newly created data
   */
  fun <T> getOrPutBufferData(key: String, provider: () -> T): T =
    getDataFromBuffer(key) ?: provider().also { putDataToBuffer(key, it) }

  /**
   * Gets data from tab or puts it if it doesn't exist.
   *
   * @param key The key to retrieve or store data for
   * @param provider A function that provides the data if it doesn't exist
   * @return The existing data or the newly created data
   */
  fun <T> getOrPutTabData(key: String, provider: () -> T): T =
    getDataFromTab(key) ?: provider().also { putDataToTab(key, it) }

  /**
   * Saves the current file.
   */
  fun saveFile()

  /**
   * Closes the current file.
   */
  fun closeFile()

  /**
   * Finds the start offset of the next word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param startIndex The index to start searching from (inclusive). Must be within the bounds of chars: [0, chars.length)
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the next word start, or null if not found
   */
  fun getNextCamelStartOffset(chars: CharSequence, startIndex: Int, count: Int = 1): Int?

  /**
   * Finds the start offset of the previous word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param endIndex The index to start searching backward from (exclusive). Must be within the bounds of chars: [0, chars.length]
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the previous word start, or null if not found
   */
  fun getPreviousCamelStartOffset(chars: CharSequence, endIndex: Int, count: Int = 1): Int?

  /**
   * Finds the end offset of the next word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param startIndex The index to start searching from (inclusive). Must be within the bounds of chars: [0, chars.length)
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the next word end, or null if not found
   */
  fun getNextCamelEndOffset(chars: CharSequence, startIndex: Int, count: Int = 1): Int?

  /**
   * Finds the end offset of the previous word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param endIndex The index to start searching backward from (exclusive). Must be within the bounds of chars: [0, chars.length]
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the previous word end, or null if not found
   */
  fun getPreviousCamelEndOffset(chars: CharSequence, endIndex: Int, count: Int = 1): Int?
}

/**
 * Sets a variable with the specified name and value.
 *
 * In Vim, this is equivalent to `let varname = value`.
 *
 * Example usage:
 * ```
 * setVariable<Int>("g:my_var", 42)
 * ```
 *
 * @param name The name of the variable, optionally prefixed with a scope (g:, b:, etc.)
 * @param value The value to set
 */
inline fun <reified T : Any> VimScope.setVariable(name: String, value: T) {
  val kType: KType = typeOf<T>()
  setVariable(name, value, kType)
}

/**
 * Retrieves a variable of the specified type and name.
 *
 * Example usage:
 * ```
 * val value: String? = getVariable<String>("myVariable")
 * ```
 *
 * @param name The name of the variable to retrieve.
 * @return The variable of type `T` if found, otherwise `null`.
 */
inline fun <reified T : Any> VimScope.getVariable(name: String): T? {
  val kType: KType = typeOf<T>()
  return getVariable(name, kType)
}
