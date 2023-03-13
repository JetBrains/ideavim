/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimStorageServiceBase
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.ij

internal class IjVimStorageService : VimStorageServiceBase() {
  val bufferToKey = mutableMapOf<String, MutableMap<String, Any?>>()

  override fun <T> getDataFromEditor(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    return editor.ij.getUserData(getOrCreateIjKey(key))
  }

  override fun <T> putDataToEditor(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    editor.ij.putUserData(getOrCreateIjKey(key), data)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> getDataFromBuffer(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    val buffer = EditorHelper.getVirtualFile(editor.ij)?.path ?: "empty path"
    return bufferToKey[buffer]?.get(key.name) as T?
  }

  override fun <T> putDataToBuffer(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    val buffer = EditorHelper.getVirtualFile(editor.ij)?.path ?: "empty path"
    var bufferStorage = bufferToKey[buffer]
    if (bufferStorage == null) {
      bufferStorage = mutableMapOf()
      bufferToKey[buffer] = bufferStorage
    }
    bufferStorage[key.name] = data
  }

  override fun <T> getDataFromTab(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    throw ExException("Tab scope is not yet supported by IdeaVim :(")
  }

  override fun <T> putDataToTab(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    throw ExException("Tab scope is not yet supported by IdeaVim :(")
  }

  private val ijKeys = mutableMapOf<String, Key<out Any?>>()

  @Suppress("UNCHECKED_CAST")
  private fun <T> getOrCreateIjKey(key: com.maddyhome.idea.vim.api.Key<T>): Key<T> {
    val storedIjKey = ijKeys[key.name]
    if (storedIjKey != null) {
      return storedIjKey as Key<T>
    }
    val newKey = Key<T>(key.name)
    ijKeys[key.name] = newKey
    return newKey
  }
}
