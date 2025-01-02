/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.mark

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import java.util.*

@CommandOrMotion(keys = ["`"], modes = [Mode.NORMAL])
class MotionGotoMarkAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override val flags: EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument !is Argument.Character) return Motion.Error

    val mark = argument.character
    return injector.motion.moveCaretToMark(caret, mark, false)
  }
}

@CommandOrMotion(keys = ["g`"], modes = [Mode.NORMAL])
class MotionGotoMarkNoSaveJumpAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument !is Argument.Character) return Motion.Error

    val mark = argument.character
    return injector.motion.moveCaretToMark(caret, mark, false)
  }
}

@CommandOrMotion(keys = ["]`"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionGotoNextMarkAction: MotionGotoRelativeMarkAction(countMultiplier = 1) {
}

@CommandOrMotion(keys = ["[`"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionGotoPreviousMarkAction: MotionGotoRelativeMarkAction(countMultiplier = -1) {
}

sealed class MotionGotoRelativeMarkAction(private val countMultiplier: Int) : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    return injector.motion.moveCaretToMarkRelative(caret, operatorArguments.count1 * countMultiplier)
  }

}
