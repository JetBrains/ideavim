/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEditorStorageServiceBase
import com.maddyhome.idea.vim.newapi.ij

class IjVimEditorStorageService : VimEditorStorageServiceBase() {
  override fun <T> getDataFromEditor(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>): T? {
    return editor.ij.getUserData(getOrCreateIjKey(key))
  }

  override fun <T> putDataToEditor(editor: VimEditor, key: com.maddyhome.idea.vim.api.Key<T>, data: T) {
    editor.ij.putUserData(getOrCreateIjKey(key), data)
  }

  private val ijKeys = mutableMapOf<String, Key<out Any?>>()
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