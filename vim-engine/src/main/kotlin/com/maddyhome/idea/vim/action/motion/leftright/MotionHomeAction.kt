/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.leftright

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.NonShiftedSpecialKeyHandler

class MotionHomeAction : NonShiftedSpecialKeyHandler() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun offset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Int {
    return injector.motion.moveCaretToLineStart(editor, caret)
  }
}
