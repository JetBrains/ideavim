/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.StorageScope
import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

class StorageScopeImpl(
  private val vimEditor: VimEditor,
) : StorageScope {
  override suspend fun <T> getWindowData(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromWindow(vimEditor, storageKey)
  }

  override suspend fun <T> putWindowData(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToWindow(vimEditor, storageKey, data)
  }

  override suspend fun <T> getBufferData(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromBuffer(vimEditor, storageKey)
  }

  override suspend fun <T> putBufferData(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToBuffer(vimEditor, storageKey, data)
  }

  override suspend fun <T> getTabData(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromTab(vimEditor, storageKey)
  }

  override suspend fun <T> putTabData(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToTab(vimEditor, storageKey, data)
  }
}
