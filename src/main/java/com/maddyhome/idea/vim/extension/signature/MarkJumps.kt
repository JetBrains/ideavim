/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.signature

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

/**
 * Moves to [line], on its first non-blank character - `]'` and `'[` and friends are line jumps and Vim puts the caret
 * on the first non-blank, not in column zero.
 */
internal fun VimEditor.jumpToLine(line: Int) {
  jumpToOffset(injector.motion.moveCaretToLineStartSkipLeading(this, line))
}

/**
 * Moves to [offset], recording the position we came from.
 *
 * Going to a mark is a jump in Vim, so `''` and `<C-O>` have to be able to bring the caret back.
 */
internal fun VimEditor.jumpToOffset(offset: Int) {
  val caret = primaryCaret()
  if (caret.offset == offset) return
  injector.jumpService.saveJumpLocation(this)
  caret.moveToOffset(offset)
}
