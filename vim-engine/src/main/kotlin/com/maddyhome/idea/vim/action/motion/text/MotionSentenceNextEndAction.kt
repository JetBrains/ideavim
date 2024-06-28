/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.text

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
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

@CommandOrMotion(keys = ["g)"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionSentenceNextEndAction : MotionSentenceEndAction(Direction.FORWARDS)

@CommandOrMotion(keys = ["g("], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionSentencePreviousEndAction : MotionSentenceEndAction(Direction.BACKWARDS)

sealed class MotionSentenceEndAction(val direction: Direction) : MotionActionHandler.ForEachCaret() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return moveCaretToNextSentenceEnd(editor, caret, direction.toInt() * operatorArguments.count1)
  }

  override val motionType: MotionType = MotionType.EXCLUSIVE
}

private fun moveCaretToNextSentenceEnd(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Motion {
  val res = injector.searchHelper.findNextSentenceEnd(editor, caret, count, false, true)
  return if (res != null) {
    editor.normalizeOffset(res, false).toMotionOrError()
  } else {
    Motion.Error
  }
}
