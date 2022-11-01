/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor

@Service
class EditorHolderService {
  var editor: Editor? = null

  companion object {
    @JvmStatic
    fun getInstance(): EditorHolderService = service()
  }
}
