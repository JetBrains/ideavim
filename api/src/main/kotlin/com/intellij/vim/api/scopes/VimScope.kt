/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.Mode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@VimPluginDsl
abstract class VimScope {
  abstract var mode: Mode

  protected abstract fun <T : Any> getVariableInternal(name: String, type: KType): T?

  @PublishedApi
  internal fun <T : Any> getVariable(name: String, type: KType): T? = getVariableInternal(name, type)

  inline fun <reified T : Any> getVariable(name: String): T? {
    val kType: KType = typeOf<T>()
    return getVariable(name, kType)
  }

  /**
   * Internal method to set a variable value.
   * 
   * @param name The name of the variable
   * @param value The value to set
   * @param type The Kotlin type of the value
   */
  protected abstract fun setVariableInternal(name: String, value: Any, type: KType)

  @PublishedApi
  internal fun setVariable(name: String, value: Any, type: KType) = setVariableInternal(name, value, type)

  /**
   * Sets a variable with the specified name and value.
   *
   * In Vim, this is equivalent to `let varname = value`.
   * Example: `let g:myvar = 42` or `let b:myvar = "text"`
   *
   * @param name The name of the variable, optionally prefixed with a scope (g:, b:, etc.)
   * @param value The value to set
   */
  inline fun <reified T : Any> setVariable(name: String, value: T) {
    val kType: KType = typeOf<T>()
    setVariable(name, value, kType)
  }

  /**
   * Locks a variable to prevent changes.
   *
   * In Vim, this is equivalent to `:lockvar varname`.
   * Example: `:lockvar g:myvar`
   *
   * @param name The name of the variable, optionally prefixed with a scope (g:, b:, etc.)
   * @param depth The lock depth (default is 1)
   */
  abstract fun lockvar(name: String, depth: Int = 1)

  /**
   * Unlocks a variable to allow changes.
   *
   * In Vim, this is equivalent to `:unlockvar varname`.
   * Example: `:unlockvar g:myvar`
   *
   * @param name The name of the variable, optionally prefixed with a scope (g:, b:, etc.)
   * @param depth The lock depth (default is 1)
   */
  abstract fun unlockvar(name: String, depth: Int = 1)

  /**
   * Checks if a variable is locked.
   *
   * In Vim, this is similar to checking the `islocked()` function.
   * Example: `if islocked("g:myvar")`
   *
   * @param name The name of the variable, optionally prefixed with a scope (g:, b:, etc.)
   * @return True if the variable is locked, false otherwise
   */
  abstract fun islocked(name: String): Boolean

  abstract fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean)
  abstract fun setOperatorFunction(name: String)
  abstract fun normal(command: String)

  @OptIn(ExperimentalContracts::class)
  fun <T> editor(block: EditorScope.() -> T): T {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return this.editorScope().block()
  }

  protected abstract fun editorScope(): EditorScope

  abstract fun mappings(block: MappingScope.() -> Unit)
  abstract fun listeners(block: ListenersScope.() -> Unit)
  abstract fun outputPanel(block: OutputPanelScope.() -> Unit)
  abstract fun modalInput(): ModalInput
  abstract fun commandLine(block: CommandLineScope.() -> Unit)

  protected abstract fun <T> getOptionValueInternal(name: String, type: KType): T?

  protected abstract fun <T> setOptionInternal(name: String, value: T, type: KType, scope: String): Boolean

  @PublishedApi
  internal fun <T : Any> getOptionValue(name: String, type: KType): T? = getOptionValueInternal(name, type)

  @PublishedApi
  internal fun <T> setGlobal(name: String, value: T, type: KType): Boolean =
    setOptionInternal(name, value, type, "global")

  @PublishedApi
  internal fun <T> setLocal(name: String, value: T, type: KType): Boolean =
    setOptionInternal(name, value, type, "local")

  @PublishedApi
  internal fun <T> set(name: String, value: T, type: KType): Boolean =
    setOptionInternal(name, value, type, "effective")

  /**
   * Gets the value of an option with the specified type.
   *
   * In Vim, options can be accessed with the `&` prefix.
   * Example: `&ignorecase` returns the value of the 'ignorecase' option.
   *
   * @param name The name of the option
   * @return The value of the option, or null if the option doesn't exist or isn't of the specified type
   */
  inline fun <reified T> getOptionValue(name: String): T? {
    val kType: KType = typeOf<T>()
    return getOptionValue(name, kType)
  }

  /**
   * Sets the global value of an option with the specified type.
   *
   * In Vim, this is equivalent to `:setglobal option=value`.
   * Example: `:setglobal ignorecase` or `let &g:ignorecase = 1`
   *
   * @param name The name of the option
   * @param value The value to set
   * @return True if the option was set successfully, false otherwise
   */
  inline fun <reified T> setGlobal(name: String, value: T): Boolean {
    val kType: KType = typeOf<T>()
    return setGlobal(name, value, kType)
  }

  /**
   * Sets the local value of an option with the specified type.
   *
   * In Vim, this is equivalent to `:setlocal option=value`.
   * Example: `:setlocal ignorecase` or `let &l:ignorecase = 1`
   *
   * @param name The name of the option
   * @param value The value to set
   * @return True if the option was set successfully, false otherwise
   */
  inline fun <reified T> setLocal(name: String, value: T): Boolean {
    val kType: KType = typeOf<T>()
    return setLocal(name, value, kType)
  }

  /**
   * Sets the effective value of an option with the specified type.
   *
   * In Vim, this is equivalent to `:set option=value`.
   * Example: `:set ignorecase` or `let &ignorecase = 1`
   *
   * @param name The name of the option
   * @param value The value to set
   * @return True if the option was set successfully, false otherwise
   */
  inline fun <reified T> set(name: String, value: T): Boolean {
    val kType: KType = typeOf<T>()
    return set(name, value, kType)
  }

  /**
   * Resets an option to its default value.
   *
   * In Vim, this is equivalent to `:set option&`.
   * Example: `:set ignorecase&` resets the 'ignorecase' option to its default value.
   *
   * @param name The name of the option
   * @return True if the option was reset successfully, false otherwise
   */
  abstract fun resetOptionToDefault(name: String): Boolean

  /**
   * Gets the number of tabs in the current window.
   */
  abstract val tabCount: Int

  /**
   * The index of the current tab or null if there is no tab selected or no tabs are open
   */
  abstract val currentTabIndex: Int?

  /**
   * Removes a tab at the specified index and selects another tab.
   *
   * @param indexToDelete The index of the tab to delete
   * @param indexToSelect The index of the tab to select after deletion
   */
  abstract fun removeTabAt(indexToDelete: Int, indexToSelect: Int)

  /**
   * Moves the current tab to the specified index.
   *
   * @param index The index to move the current tab to
   * @throws IllegalStateException if there is no tab selected or no tabs are open
   */
  abstract fun moveCurrentTabToIndex(index: Int)

  /**
   * Closes all tabs except the current one.
   *
   * @throws IllegalStateException if there is no tab selected
   */
  abstract fun closeAllExceptCurrentTab()

  /**
   * Checks if a pattern matches a text.
   *
   * @param pattern The regular expression pattern to match
   * @param text The text to check against the pattern
   * @param ignoreCase Whether to ignore case when matching
   * @return True if the pattern matches the text, false otherwise
   */
  abstract fun matches(pattern: String, text: String?, ignoreCase: Boolean = false): Boolean

  /**
   * Finds all matches of a pattern in a text.
   *
   * @param text The text to search in
   * @param pattern The regular expression pattern to search for
   * @return A list of pairs representing the start and end offsets of each match
   */
  abstract fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>>

  /**
   * Selects the next window in the editor.
   */
  abstract fun selectNextWindow()

  /**
   * Selects the previous window in the editor.
   */
  abstract fun selectPreviousWindow()

  /**
   * Selects a window by its index.
   *
   * @param index The index of the window to select (1-based).
   */
  abstract fun selectWindow(index: Int)

  /**
   * Splits the current window vertically and optionally opens a file in the new window.
   *
   * @param filename The name of the file to open in the new window. If null, the new window will show the same file.
   */
  abstract fun splitWindowVertically(filename: String? = null)

  /**
   * Splits the current window horizontally and optionally opens a file in the new window.
   *
   * @param filename The name of the file to open in the new window. If null, the new window will show the same file.
   */
  abstract fun splitWindowHorizontally(filename: String? = null)

  /**
   * Closes all windows except the current one.
   */
  abstract fun closeAllExceptCurrentWindow()

  /**
   * Closes the current window.
   */
  abstract fun closeCurrentWindow()

  /**
   * Closes all windows in the editor.
   */
  abstract fun closeAllWindows()

  /**
   * Parses and executes the given Vimscript string. It can be used to execute
   * ex commands, such as `:set`, `:map`, etc.
   *
   * @param script The Vimscript string to execute
   * @return The result of the execution, which can be Success or Error
   */
  abstract fun execute(script: String): Boolean

  abstract fun command(command: String, block: VimScope.(String) -> Unit)

  /**
   * Gets keyed data from a Vim window.
   *
   * IdeaVim's editor is equivalent to Vim's window, which is an editor view on a buffer.
   * Vim stores window scoped variables (`w:`) and local-to-window options per-window.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  abstract fun <T> getDataFromWindow(key: String): T?

  /**
   * Stores keyed user data in a Vim window.
   *
   * IdeaVim's editor is equivalent to Vim's window, which is an editor view on a buffer.
   * Vim stores window scoped variables (`w:`) and local-to-window options per-window.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  abstract fun <T> putDataToWindow(key: String, data: T)

  /**
   * Gets data from buffer.
   * Vim stores there buffer scoped (`b:`) variables and local options.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  abstract fun <T> getDataFromBuffer(key: String): T?

  /**
   * Puts data to buffer.
   * Vim stores there buffer scoped (`b:`) variables and local options.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  abstract fun <T> putDataToBuffer(key: String, data: T)

  /**
   * Gets data from tab (group of windows).
   * Vim stores there tab page scoped (`t:`) variables.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  abstract fun <T> getDataFromTab(key: String): T?

  /**
   * Puts data to tab (group of windows).
   * Vim stores there tab page scoped (`t:`) variables.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  abstract fun <T> putDataToTab(key: String, data: T)

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
   *
   * In Vim, this is equivalent to the `:w` command.
   */
  abstract fun saveFile()

  /**
   * Closes the current file.
   *
   * In Vim, this is equivalent to the `:q` command.
   */
  abstract fun closeFile()
}
