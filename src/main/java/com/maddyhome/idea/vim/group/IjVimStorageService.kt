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
import com.maddyhome.idea.vim.newapi.ij

internal class IjVimStorageService : VimStorageServiceBase() {
  override fun <T> getDataFromWindow(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    return editor.ij.getUserData(key.ij)
  }

  override fun <T> putDataToWindow(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    editor.ij.putUserData(key.ij, data)
  }

  override fun <T> getDataFromBuffer(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    return editor.ij.document.getUserData(key.ij)
  }

  override fun <T> putDataToBuffer(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    editor.ij.document.putUserData(key.ij, data)
  }

  override fun <T> getDataFromTab(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    throw ExException("Tab scope is not yet supported by IdeaVim :(")
  }

  override fun <T> putDataToTab(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    throw ExException("Tab scope is not yet supported by IdeaVim :(")
  }

  private val ijKeys = mutableMapOf<String, Key<out Any?>>()

  @Suppress("UNCHECKED_CAST")
  private val <T> com.maddyhome.idea.vim.api.Key<T>.ij: Key<T>
    get(): Key<T> {
      val storedIjKey = ijKeys[this.name]
      if (storedIjKey != null) {
        return storedIjKey as Key<T>
      }
      val newKey = Key<T>(this.name)
      ijKeys[this.name] = newKey
      return newKey
    }
}
