/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor

/**
 * Key handling for IdeaVim should be updated to editorHandler usage. In this case this class can be safely removed.
 */

interface AceJumpService {
  fun isActive(editor: Editor): Boolean

  companion object {
    fun getInstance(): AceJumpService? = ApplicationManager.getApplication().getService(AceJumpService::class.java)
  }
}
