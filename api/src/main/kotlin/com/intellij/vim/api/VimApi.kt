/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.models.Mode
import com.intellij.vim.api.scopes.CommandScope
import com.intellij.vim.api.scopes.DigraphScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.ModalInput
import com.intellij.vim.api.scopes.OptionScope
import com.intellij.vim.api.scopes.OutputPanelScope
import com.intellij.vim.api.scopes.StorageScope
import com.intellij.vim.api.scopes.TabScope
import com.intellij.vim.api.scopes.TextObjectScope
import com.intellij.vim.api.scopes.TextScope
import com.intellij.vim.api.scopes.VariableScope
import com.intellij.vim.api.scopes.VimApiDsl
import com.intellij.vim.api.scopes.get
import com.intellij.vim.api.scopes.set
import com.intellij.vim.api.scopes.commandline.CommandLineScope
import com.intellij.vim.api.scopes.editor.EditorScope
import org.jetbrains.annotations.ApiStatus

/**
 * Entry point of the Vim API
 *
 * The API is currently in experimental status and not suggested to be used.
 */
@ApiStatus.Experimental
@VimApiDsl
interface VimApi {
  /**
   * Represents the current mode in Vim (read-only).
   *
   * To change modes, use [normal] with the appropriate key sequence:
   * - `normal("i")` — enter Insert mode
   * - `normal("<Esc>")` — exit to Normal mode (like pressing Escape)
   * - `normal("v")` — enter Visual character mode
   * - `normal("V")` — enter Visual line mode
   *
   * Example usage:
   * ```kotlin
   * val currentMode = mode
   * if (currentMode == Mode.INSERT) {
   *   normal("<Esc>")  // exit to normal
   * }
   * ```
   */
  val mode: Mode

  /**
   * Provides access to Vim variables.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * val name = variables { get<String>("g:name") }
   *
   * // Direct object style
   * variables().set("g:x", 1)
   * ```
   *
   * @param block The code block to execute within the variable scope
   * @return The result of the block execution
   */
  fun <T> variables(block: VariableScope.() -> T): T

  /**
   * Provides direct access to Vim variables scope.
   *
   * @return The VariableScope for chaining
   */
  fun variables(): VariableScope

  /**
   * Provides access to command registration and operator functions.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * commands {
   *     register("MyCommand") { cmd, startLine, endLine ->
   *         println("Command executed: $cmd")
   *     }
   * }
   *
   * // Direct object style
   * commands().exportOperatorFunction("MyOperator") { true }
   * ```
   *
   * @param block The code block to execute within the command scope
   * @return The result of the block execution
   */
  fun <T> commands(block: CommandScope.() -> T): T

  /**
   * Provides direct access to command scope.
   *
   * @return The CommandScope for chaining
   */
  fun commands(): CommandScope

  /**
   * Executes normal mode commands as if they were typed.
   *
   * In Vim, this is equivalent to the `:normal!` command (without remapping).
   * Supports Vim key notation: `<Esc>`, `<CR>`, `<C-O>`, `<C-V>`, etc.
   *
   * Example usage:
   * ```kotlin
   * normal("gg")     // Go to the first line
   * normal("dw")     // Delete word
   * normal("i")      // Enter Insert mode
   * normal("<Esc>")  // Exit to Normal mode (like pressing Escape)
   * normal("v")      // Enter Visual character mode
   * normal("V")      // Enter Visual line mode
   * ```
   *
   * @param command The normal mode command string to execute
   */
  suspend fun normal(command: String)

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
  suspend fun <T> editor(block: suspend EditorScope.() -> T): T

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
  suspend fun <T> forEachEditor(block: suspend EditorScope.() -> T): List<T>

  /**
   * Provides access to key mapping functionality.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * mappings {
   *    nmap("jk", "<Esc>")
   * }
   *
   * // Chained style
   * mappings().nmap("jk", "<Esc>")
   * ```
   *
   * @param block The code block to execute within the mapping scope
   * @return The MappingScope for chaining
   */
  fun <T> mappings(block: MappingScope.() -> T): T
  fun mappings(): MappingScope

  /**
   * Provides access to text object registration.
   *
   * Text objects are selections that can be used with operators (like `d`, `c`, `y`)
   * or in visual mode. Examples include `iw` (inner word), `ap` (a paragraph), etc.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * textObjects {
   *     register("ae") { count ->
   *         TextObjectRange.CharacterWise(0, editor { read { textLength.toInt() } })
   *     }
   * }
   *
   * // Chained style
   * textObjects().register("ae") { count ->
   *     TextObjectRange.CharacterWise(0, editor { read { textLength.toInt() } })
   * }
   * ```
   *
   * @param block The code block to execute within the text object scope
   * @return The TextObjectScope for chaining
   */
  fun <T> textObjects(block: TextObjectScope.() -> T): T
  fun textObjects(): TextObjectScope

//  /**
//   * Provides access to event listener functionality.
//   *
//   * Example usage:
//   * ```kotlin
//   * listeners {
//   *     // Register a listener for mode changes
//   *     onModeChange { oldMode ->
//   *         println("Mode changed from $oldMode")
//   *     }
//   * }
//   * ```
//   *
//   * @param block The code block to execute within the listeners scope
//   */
//  fun listeners(block: ListenersScope.() -> Unit)

  /**
   * Provides access to Vim's output panel functionality.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * outputPanel {
   *     // Print a message to the output panel
   *     setText("Hello from IdeaVim plugin!")
   * }
   *
   * // Chained style
   * outputPanel().setText("Hello from IdeaVim plugin!")
   * ```
   *
   * @param block The code block to execute within the output panel scope
   * @return The OutputPanelScope for chaining
   */
  suspend fun <T> outputPanel(block: suspend OutputPanelScope.() -> T): T
  suspend fun outputPanel(): OutputPanelScope

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
  suspend fun modalInput(): ModalInput

  /**
   * Provides access to Vim's command line functionality.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * commandLine {
   *    // get current command line text
   *    read {
   *      // executed under read lock
   *      text
   *    }
   * }
   *
   * // Chained style
   * commandLine().read { text }
   * ```
   *
   * @param block The code block to execute with command line scope
   * @return The CommandLineScope for chaining
   */
  suspend fun <T> commandLine(block: suspend CommandLineScope.() -> T): T
  suspend fun commandLine(): CommandLineScope

  /**
   * Provides access to Vim's options functionality.
   *
   * Example usage:
   * ```kotlin
   * // Get option value
   * val history = option { get<Int>("history") }
   * 
   * // Set option value and return result
   * val wasSet = option { 
   *     set("number", true)
   *     true
   * }
   * 
   * // Multiple operations
   * option {
   *     set("ignorecase", true)
   *     append("virtualedit", "block")
   * }
   * ```
   *
   * @param block The code block to execute within the option scope
   * @return The result of the block execution
   */
  suspend fun <T> option(block: suspend OptionScope.() -> T): T

  /**
   * Provides access to Vim's digraph functionality.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * digraph {
   *     // Add a new digraph
   *     add("a:", 'ä')
   * }
   *
   * // Chained style
   * digraph().add('a', ':', 228)
   * ```
   *
   * @param block The code block to execute within the digraph scope
   * @return The DigraphScope for chaining
   */
  suspend fun <T> digraph(block: suspend DigraphScope.() -> T): T
  suspend fun digraph(): DigraphScope

  /**
   * Provides access to tab management.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * val count = tabs { count }
   *
   * // Direct object style
   * tabs().closeAllExceptCurrent()
   * ```
   *
   * @param block The code block to execute within the tab scope
   * @return The result of the block execution
   */
  suspend fun <T> tabs(block: suspend TabScope.() -> T): T

  /**
   * Provides direct access to tab scope.
   *
   * @return The TabScope for chaining
   */
  suspend fun tabs(): TabScope

  /**
   * Provides access to text pattern matching and word-boundary utilities.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * val found = text { matches("\\w+", "hello") }
   *
   * // Direct object style
   * val offset = text().getNextCamelStartOffset(chars, 0)
   * ```
   *
   * @param block The code block to execute within the text scope
   * @return The result of the block execution
   */
  suspend fun <T> text(block: suspend TextScope.() -> T): T

  /**
   * Provides direct access to text scope.
   *
   * @return The TextScope for chaining
   */
  suspend fun text(): TextScope

  // Window management APIs commented out — see IJPL-235369.
  // After switching windows, FileEditorManager.getSelectedTextEditor() does not
  // immediately reflect the change because EditorsSplitters.currentCompositeFlow
  // is derived asynchronously (flatMapLatest + stateIn), and there is no way to
  // observe when the propagation completes.
  //
  // /**
  //  * Selects the next window in the editor.
  //  */
  // fun selectNextWindow()
  //
  // /**
  //  * Selects the previous window in the editor.
  //  */
  // fun selectPreviousWindow()
  //
  // /**
  //  * Selects a window by its index.
  //  *
  //  * @param index The index of the window to select (1-based).
  //  */
  // fun selectWindow(index: Int)
  //
  // /**
  //  * Splits the current window vertically and optionally opens a file in the new window.
  //  *
  //  * @param filePath Path of the file to open in the new window. If null, the new window will show the same file.
  //  */
  // fun splitWindowVertically(filePath: Path? = null)
  //
  // /**
  //  * Splits the current window horizontally and optionally opens a file in the new window.
  //  *
  //  * @param filePath Path of the file to open in the new window. If null, the new window will show the same file.
  //  */
  // fun splitWindowHorizontally(filePath: Path? = null)
  //
  // /**
  //  * Closes all windows except the current one.
  //  */
  // fun closeAllExceptCurrentWindow()
  //
  // /**
  //  * Closes the current window.
  //  */
  // fun closeCurrentWindow()
  //
  // /**
  //  * Closes all windows in the editor.
  //  */
  // fun closeAllWindows()

  /**
   * Parses and executes the given Vimscript string.
   *
   * @param script The Vimscript string to execute
   * @return The result of the execution, which can be Success or Error
   */
  suspend fun execute(script: String): Boolean


  /**
   * Provides access to keyed data storage for windows, buffers, and tabs.
   *
   * Example usage:
   * ```kotlin
   * // Lambda style
   * val data = storage { getWindowData<String>("myKey") }
   *
   * // Direct object style
   * storage().putWindowData("myKey", "value")
   * ```
   *
   * @param block The code block to execute within the storage scope
   * @return The result of the block execution
   */
  suspend fun <T> storage(block: suspend StorageScope.() -> T): T

  /**
   * Provides direct access to storage scope.
   *
   * @return The StorageScope for chaining
   */
  suspend fun storage(): StorageScope

  /**
   * Saves the current file.
   */
  suspend fun saveFile()

  /**
   * Closes the current file.
   */
  suspend fun closeFile()

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
inline fun <reified T : Any> VimApi.setVariable(name: String, value: T) {
  variables().set(name, value)
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
inline fun <reified T : Any> VimApi.getVariable(name: String): T? {
  return variables().get(name)
}
