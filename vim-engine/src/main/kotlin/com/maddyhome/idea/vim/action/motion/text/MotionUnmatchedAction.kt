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
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.findUnmatchedBlock
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

sealed class MotionUnmatchedAction(private val motionChar: Char) : MotionActionHandler.ForEachCaret() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return moveCaretToUnmatchedBlock(editor, caret, operatorArguments.count1, motionChar)?.toMotionOrError()
      ?: Motion.Error
  }
}

@CommandOrMotion(keys = ["]}"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionUnmatchedBraceCloseAction : MotionUnmatchedAction('}')

@CommandOrMotion(keys = ["[{"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionUnmatchedBraceOpenAction : MotionUnmatchedAction('{')

@CommandOrMotion(keys = ["])"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionUnmatchedParenCloseAction : MotionUnmatchedAction(')')

@CommandOrMotion(keys = ["[("], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionUnmatchedParenOpenAction : MotionUnmatchedAction('(')

private fun moveCaretToUnmatchedBlock(editor: VimEditor, caret: ImmutableVimCaret, count: Int, type: Char): Int? {
  return if (editor.currentCaret().offset == 0 && count < 0 || editor.currentCaret().offset >= editor.fileSize() - 1 && count > 0) {
    null
  } else {
    val res = findUnmatchedBlock(editor, caret.offset, type, count) ?: return null
    return editor.normalizeOffset(res, false)
  }
}
