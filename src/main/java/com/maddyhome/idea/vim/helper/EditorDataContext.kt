/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder

@Deprecated("Do not use context wrappers, use existing provided contexts. If no context available, use `injector.getExecutionContextManager().getEditorExecutionContext(editor)`")
internal class EditorDataContext @Deprecated("Please use `init` method") constructor(
  private val editor: Editor,
  private val editorContext: DataContext,
  private val contextDelegate: DataContext? = null,
) : DataContext, UserDataHolder {
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
    else -> editorContext.getData(dataId) ?: contextDelegate?.getData(dataId)
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
      val editorContext = EditorUtil.getEditorDataContext(editor)
      return if (contextDelegate is EditorDataContext) {
        if (editor === contextDelegate.editor) {
          contextDelegate
        } else {
          EditorDataContext(editor, editorContext, contextDelegate.contextDelegate)
        }
      } else {
        EditorDataContext(editor, editorContext, contextDelegate)
      }
    }
  }
}
