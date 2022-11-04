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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.ShiftedArrowKeyHandler

/**
 * @author Alex Plate
 */

class MotionShiftRightAction : ShiftedArrowKeyHandler(true) {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motionWithKeyModel(editor: VimEditor, caret: VimCaret, context: ExecutionContext, cmd: Command) {
    val vertical = injector.motion.getOffsetOfHorizontalMotion(editor, caret, cmd.count, true)
    caret.moveToOffset(vertical)
  }

  override fun motionWithoutKeyModel(editor: VimEditor, context: ExecutionContext, cmd: Command) {
    val caret = editor.currentCaret()
    val newOffset = injector.motion.findOffsetOfNextWord(editor, caret.offset.point, cmd.count, false)
    if (newOffset is Motion.AbsoluteOffset) {
      caret.moveToOffset(newOffset.offset)
    }
  }
}
