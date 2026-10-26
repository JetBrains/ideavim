/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.quickfix

import com.maddyhome.idea.vim.api.QuickFixJump
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * Reports a quickfix jump: E42 for an empty list, E553 when there's no item to move to,
 * otherwise `(2 of 3): text` in the editor that was jumped to.
 */
internal fun reportQuickFixJump(jump: QuickFixJump): ExecutionResult {
  when (jump) {
    QuickFixJump.EmptyList -> throw exExceptionMessage("E42")
    QuickFixJump.NoMoreItems -> throw exExceptionMessage("E553")
    is QuickFixJump.Moved -> showPosition(jump)
  }
  return ExecutionResult.Success
}

/**
 * Shows `(2 of 3): text`, without the leading whitespace of the text, in the editor that was jumped to.
 */
internal fun showPosition(jump: QuickFixJump.Moved) {
  val editor = jump.editor ?: return
  injector.messages.showMessage(editor, "(${jump.number} of ${jump.size}): ${jump.item.text.trimStart()}")
}
