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
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler

@CommandOrMotion(keys = ["gE"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionBigWordEndLeftAction : WordEndAction(Direction.BACKWARDS, true)

@CommandOrMotion(keys = ["E"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionBigWordEndRightAction : WordEndAction(Direction.FORWARDS, true)

@CommandOrMotion(keys = ["ge"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionWordEndLeftAction : WordEndAction(Direction.BACKWARDS, false)

@CommandOrMotion(keys = ["e"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionWordEndRightAction : WordEndAction(Direction.FORWARDS, false)

sealed class WordEndAction(val direction: Direction, val bigWord: Boolean) : MotionActionHandler.ForEachCaret() {
  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return moveCaretToNextWordEnd(editor, caret, direction.toInt() * operatorArguments.count1, bigWord)
  }

  override val motionType: MotionType = MotionType.INCLUSIVE
}

private fun moveCaretToNextWordEnd(editor: VimEditor, caret: ImmutableVimCaret, count: Int, bigWord: Boolean): Motion {
  if (caret.offset == 0 && count < 0 || caret.offset >= editor.fileSize() - 1 && count > 0) {
    return Motion.Error
  }

  // If we are doing this move as part of a change command (e.q. cw), we need to count the current end of
  // word if the cursor happens to be on the end of a word already. If this is a normal move, we don't count
  // the current word.
  val pos = injector.searchHelper.findNextWordEnd(editor, caret.offset, count, bigWord, false)
  return if (pos == -1) {
    if (count < 0) {
      AbsoluteOffset(injector.motion.moveCaretToLineStart(editor, 0))
    } else {
      AbsoluteOffset(injector.motion.moveCaretToLineEnd(editor, editor.lineCount() - 1, false))
    }
  } else {
    AbsoluteOffset(pos)
  }
}
