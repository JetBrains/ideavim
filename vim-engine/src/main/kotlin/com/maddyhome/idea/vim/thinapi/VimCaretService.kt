/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor

interface VimCaretService {
  fun addCaret(offset: Int, editor: VimEditor): VimCaret?
  fun removeCaret(caret: VimCaret, editor: VimEditor)
}