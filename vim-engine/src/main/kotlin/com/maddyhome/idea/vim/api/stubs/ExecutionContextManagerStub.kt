/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ExecutionContextManager
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.diagnostic.vimLogger

class ExecutionContextManagerStub : ExecutionContextManager {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("ExecutionContextManagerStub is used. Please replace it with your own implementation of ExecutionContextManager.")
  }

  override fun onEditor(editor: VimEditor, prevContext: ExecutionContext?): ExecutionContext.Editor {
    TODO("Not yet implemented")
  }

  override fun onCaret(caret: VimCaret, prevContext: ExecutionContext.Editor): ExecutionContext.CaretAndEditor {
    TODO("Not yet implemented")
  }
}
