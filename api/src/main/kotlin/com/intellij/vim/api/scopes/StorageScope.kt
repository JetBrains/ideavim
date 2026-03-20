/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Scope that provides access to keyed data storage for windows, buffers, and tabs.
 *
 * Example usage:
 * ```kotlin
 * // Lambda style
 * val data = api.storage { getWindowData<String>("myKey") }
 *
 * // Direct object style
 * api.storage().putWindowData("myKey", "value")
 * ```
 */
@VimApiDsl
interface StorageScope {
  /**
   * Gets keyed data from a Vim window.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  suspend fun <T> getWindowData(key: String): T?

  /**
   * Stores keyed user data in a Vim window.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  suspend fun <T> putWindowData(key: String, data: T)

  /**
   * Gets data from buffer.
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  suspend fun <T> getBufferData(key: String): T?

  /**
   * Puts data to buffer.
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  suspend fun <T> putBufferData(key: String, data: T)

  /**
   * Gets data from tab (group of windows).
   *
   * @param key The key to retrieve data for
   * @return The data associated with the key, or null if no data is found
   */
  suspend fun <T> getTabData(key: String): T?

  /**
   * Puts data to tab (group of windows).
   *
   * @param key The key to store data for
   * @param data The data to store
   */
  suspend fun <T> putTabData(key: String, data: T)

  /**
   * Gets data from window or puts it if it doesn't exist.
   *
   * @param key The key to retrieve or store data for
   * @param provider A function that provides the data if it doesn't exist
   * @return The existing data or the newly created data
   */
  suspend fun <T> getOrPutWindowData(key: String, provider: () -> T): T =
    getWindowData(key) ?: provider().also { putWindowData(key, it) }

  /**
   * Gets data from buffer or puts it if it doesn't exist.
   *
   * @param key The key to retrieve or store data for
   * @param provider A function that provides the data if it doesn't exist
   * @return The existing data or the newly created data
   */
  suspend fun <T> getOrPutBufferData(key: String, provider: () -> T): T =
    getBufferData(key) ?: provider().also { putBufferData(key, it) }

  /**
   * Gets data from tab or puts it if it doesn't exist.
   *
   * @param key The key to retrieve or store data for
   * @param provider A function that provides the data if it doesn't exist
   * @return The existing data or the newly created data
   */
  suspend fun <T> getOrPutTabData(key: String, provider: () -> T): T =
    getTabData(key) ?: provider().also { putTabData(key, it) }
}
