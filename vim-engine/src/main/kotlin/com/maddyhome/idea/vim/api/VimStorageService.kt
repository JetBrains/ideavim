/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimStorageService {
  /**
   * Gets data from editor (in Vim it is called window).
   * Vim stores there window scoped (`w:`) variables and local options.
   *
   * NOTE: data should remain in window even if it is moved to another split
   * @param editor editor/window to get the value from
   * @param key key
   */
  public fun <T> getDataFromEditor(editor: VimEditor, key: Key<T>): T?

  /**
   * Puts data to editor (in Vim it is called window).
   * Vim stores there window scoped (`w:`) variables and local options.
   *
   * NOTE: data should remain in window even if it is moved to another split
   * @param editor editor/window to store the value
   * @param key key
   * @param data data to store
   */
  public fun <T> putDataToEditor(editor: VimEditor, key: Key<T>, data: T)

  /**
   * Gets data from buffer
   * Vim stores there buffer scoped (`b:`) variables and local options.
   *
   * @param editor editor/window with the buffer opened
   * @param key key
   */
  public fun <T> getDataFromBuffer(editor: VimEditor, key: Key<T>): T?

  /**
   * Puts data to buffer
   * Vim stores there buffer scoped (`b:`) variables and local options.
   *
   * @param editor editor/window with the buffer opened
   * @param key key
   * @param data data to store
   */
  public fun <T> putDataToBuffer(editor: VimEditor, key: Key<T>, data: T)

  /**
   * Gets data from tab (group of windows)
   * Vim stores there tab page scoped (`t:`) variables
   *
   * @param editor editor/window in the tap page
   * @param key key
   */
  public fun <T> getDataFromTab(editor: VimEditor, key: Key<T>): T?

  /**
   * Puts data to tab (group of windows)
   * Vim stores there tab page scoped (`t:`) variables
   *
   * @param editor editor/window in the tap page
   * @param key key
   * @param data data to store
   */
  public fun <T> putDataToTab(editor: VimEditor, key: Key<T>, data: T)
}

public data class Key<T>(val name: String)

public fun <T> VimStorageService.getOrPutEditorData(editor: VimEditor, key: Key<T>, provider: () -> T): T =
  getDataFromEditor(editor, key) ?: provider().also { putDataToEditor(editor, key, it) }

public fun <T> VimStorageService.getOrPutBufferData(editor: VimEditor, key: Key<T>, provider: () -> T): T =
  getDataFromBuffer(editor, key) ?: provider().also { putDataToBuffer(editor, key, it) }

public fun <T> VimStorageService.getOrPutTabData(editor: VimEditor, key: Key<T>, provider: () -> T): T =
  getDataFromTab(editor, key) ?: provider().also { putDataToTab(editor, key, it) }
