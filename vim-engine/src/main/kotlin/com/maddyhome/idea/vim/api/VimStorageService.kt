/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimStorageService {
  /**
   * Gets keyed data from a Vim window
   *
   * IdeaVim's [VimEditor] is equivalent to Vim's window, which is an editor view on a buffer. Vim stores window scoped
   * variables (`w:`) and local-to-window options per-window.
   *
   * NOTE: data should remain in window even if it is moved to another split
   * @param editor editor/window to get the value from
   * @param key key
   */
  fun <T> getDataFromWindow(editor: VimEditor, key: Key<T>): T?

  /**
   * Stores keyed user data in a Vim window
   *
   * IdeaVim's [VimEditor] is equivalent to Vim's window, which is an editor view on a buffer. Vim stores window scoped
   * variables (`w:`) and local-to-window options per-window.
   *
   * NOTE: data should remain in window even if it is moved to another split
   * @param editor editor/window to store the value
   * @param key key
   * @param data data to store
   */
  fun <T> putDataToWindow(editor: VimEditor, key: Key<T>, data: T)

  /**
   * Gets data from buffer
   * Vim stores there buffer scoped (`b:`) variables and local options.
   *
   * @param editor editor/window with the buffer opened
   * @param key key
   */
  fun <T> getDataFromBuffer(editor: VimEditor, key: Key<T>): T?

  /**
   * Puts data to buffer
   * Vim stores there buffer scoped (`b:`) variables and local options.
   *
   * @param editor editor/window with the buffer opened
   * @param key key
   * @param data data to store
   */
  fun <T> putDataToBuffer(editor: VimEditor, key: Key<T>, data: T)

  /**
   * Gets data from tab (group of windows)
   * Vim stores there tab page scoped (`t:`) variables
   *
   * @param editor editor/window in the tap page
   * @param key key
   */
  fun <T> getDataFromTab(editor: VimEditor, key: Key<T>): T?

  /**
   * Puts data to tab (group of windows)
   * Vim stores there tab page scoped (`t:`) variables
   *
   * @param editor editor/window in the tap page
   * @param key key
   * @param data data to store
   */
  fun <T> putDataToTab(editor: VimEditor, key: Key<T>, data: T)
}

data class Key<T>(val name: String)

fun <T> VimStorageService.getOrPutWindowData(editor: VimEditor, key: Key<T>, provider: () -> T): T =
  getDataFromWindow(editor, key) ?: provider().also { putDataToWindow(editor, key, it) }

fun <T> VimStorageService.getOrPutBufferData(editor: VimEditor, key: Key<T>, provider: () -> T): T =
  getDataFromBuffer(editor, key) ?: provider().also { putDataToBuffer(editor, key, it) }

fun <T> VimStorageService.getOrPutTabData(editor: VimEditor, key: Key<T>, provider: () -> T): T =
  getDataFromTab(editor, key) ?: provider().also { putDataToTab(editor, key, it) }
