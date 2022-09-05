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
package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder

class EditorDataContext @Deprecated("Please use `init` method") constructor(
  private val editor: Editor,
  private val contextDelegate: DataContext? = null,
) : DataContext, UserDataHolder {

  internal var newTypingDelegate = false

  /**
   * Returns the object corresponding to the specified data identifier. Some of the supported data identifiers are
   * defined in the [PlatformDataKeys] class.
   *
   * @param dataId the data identifier for which the value is requested.
   * @return the value, or null if no value is available in the current context for this identifier.
   */
  override fun getData(dataId: String): Any? = when {
    PlatformDataKeys.EDITOR.name == dataId -> editor
    PlatformDataKeys.PROJECT.name == dataId -> editor.project
    PlatformDataKeys.VIRTUAL_FILE.name == dataId -> EditorHelper.getVirtualFile(editor)
    NEW_DELEGATE.name == dataId -> newTypingDelegate
    else -> contextDelegate?.getData(dataId)
  }

  override fun <T : Any?> getUserData(key: Key<T>): T? {
    return if (contextDelegate is UserDataHolder) {
      contextDelegate.getUserData(key)
    } else {
      null
    }
  }

  override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    if (contextDelegate is UserDataHolder) {
      contextDelegate.putUserData(key, value)
    }
  }

  companion object {
    @Suppress("DEPRECATION")
    @JvmStatic
    fun init(editor: Editor, contextDelegate: DataContext? = null): EditorDataContext {
      return if (contextDelegate is EditorDataContext) {
        if (editor === contextDelegate.editor) {
          contextDelegate
        } else {
          EditorDataContext(editor, contextDelegate.contextDelegate)
        }
      } else {
        EditorDataContext(editor, contextDelegate)
      }
    }
  }
}

internal val NEW_DELEGATE = DataKey.create<Boolean>("IdeaVim.NEW_DELEGATE")
