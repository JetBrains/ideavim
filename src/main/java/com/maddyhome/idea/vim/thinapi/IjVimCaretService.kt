/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

@Service
class IjVimCaretService : VimCaretService {
  override fun addCaret(
    offset: Int,
    editor: VimEditor,
  ): VimCaret? {
    val ijEditor = editor.ij

    return ijEditor.caretModel.addCaret(
      ijEditor.offsetToVisualPosition(offset)
    )?.vim
  }

  override fun removeCaret(caret: VimCaret, editor: VimEditor) {
    editor.ij.caretModel.removeCaret(caret.ij)
  }
}