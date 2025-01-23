/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * This extension point is available only for the IdeaVim internally
 */
internal interface IdeaVimDisablerExtensionPoint {
  fun isDisabledForEditor(editor: Editor): Boolean

  companion object {
    private val EP_NAME = ExtensionPointName.create<IdeaVimDisablerExtensionPoint>("IdeaVIM.internal.disabler")

    fun isDisabledForEditor(editor: Editor): Boolean {
      return EP_NAME.extensionList.any { it.isDisabledForEditor(editor) }
    }
  }
}
