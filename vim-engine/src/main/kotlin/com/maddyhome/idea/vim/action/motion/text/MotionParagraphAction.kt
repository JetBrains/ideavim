/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.text

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

sealed class MotionParagraphAction(val direction: Direction) : MotionActionHandler.ForEachCaret() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return moveCaretToNextParagraph(editor, caret, direction.toInt() * operatorArguments.count1).toMotionOrError()
  }

  override val motionType: MotionType = MotionType.EXCLUSIVE
}

class MotionParagraphNextAction : MotionParagraphAction(Direction.FORWARDS)
class MotionParagraphPreviousAction : MotionParagraphAction(Direction.BACKWARDS)

private fun moveCaretToNextParagraph(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
  var res = injector.searchHelper.findNextParagraph(editor, caret, count, false)
  res = if (res >= 0) {
    editor.normalizeOffset(res, true)
  } else {
    -1
  }
  return res
}
