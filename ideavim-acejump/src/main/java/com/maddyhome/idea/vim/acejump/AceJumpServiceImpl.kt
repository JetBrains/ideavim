/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.acejump

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.listener.AceJumpService
import org.acejump.session.SessionManager

class AceJumpServiceImpl : AceJumpService {
  override fun isActive(editor: Editor): Boolean {
    return try {
      SessionManager[editor] != null
    } catch (e: Throwable) {
      // In case of any exception
      false
    }
  }
}
