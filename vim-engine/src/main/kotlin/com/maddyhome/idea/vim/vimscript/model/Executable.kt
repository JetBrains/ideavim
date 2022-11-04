/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor

interface Executable : VimLContext {

  var vimContext: VimLContext

  override fun getPreviousParentContext(): VimLContext {
    return vimContext
  }

  fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult
}
