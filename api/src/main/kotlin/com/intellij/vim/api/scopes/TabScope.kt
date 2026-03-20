/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Scope that provides access to tab management.
 *
 * Example usage:
 * ```kotlin
 * // Lambda style
 * val count = api.tabs { count }
 *
 * // Direct object style
 * api.tabs().closeAllExceptCurrent()
 * ```
 */
@VimApiDsl
interface TabScope {
  /**
   * Gets the number of tabs in the current window.
   */
  val count: Int

  /**
   * The index of the current tab or null if there is no tab selected or no tabs are open.
   */
  val currentIndex: Int?

  /**
   * Removes a tab at the specified index and selects another tab.
   *
   * @param indexToDelete The index of the tab to delete
   * @param indexToSelect The index of the tab to select after deletion
   */
  suspend fun removeAt(indexToDelete: Int, indexToSelect: Int)

  /**
   * Moves the current tab to the specified index.
   *
   * @param index The index to move the current tab to
   * @throws IllegalStateException if there is no tab selected or no tabs are open
   */
  suspend fun moveCurrentToIndex(index: Int)

  /**
   * Closes all tabs except the current one.
   *
   * @throws IllegalStateException if there is no tab selected
   */
  suspend fun closeAllExceptCurrent()
}
