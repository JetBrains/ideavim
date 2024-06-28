/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.leftright

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler

@CommandOrMotion(keys = [";"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionLastMatchCharAction : MotionLastMatchCharActionBase(MotionType.INCLUSIVE, reverse = false)

@CommandOrMotion(keys = [","], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionLastMatchCharReverseAction : MotionLastMatchCharActionBase(MotionType.EXCLUSIVE, reverse = true)

sealed class MotionLastMatchCharActionBase(defaultMotionType: MotionType, private val reverse: Boolean) :
  MotionActionHandler.ForEachCaret() {

  // injector.motion.lastFTCmd not up-to-date during construction, need to set during getOffset
  override var motionType: MotionType = defaultMotionType

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    motionType = when (injector.motion.lastFTCmd) {
      TillCharacterMotionType.LAST_SMALL_F, TillCharacterMotionType.LAST_SMALL_T -> MotionType.INCLUSIVE
      TillCharacterMotionType.LAST_F, TillCharacterMotionType.LAST_T -> MotionType.EXCLUSIVE
    }.let { if (reverse) it.reverse() else it }
    val repeatLastMatchChar =
      injector.motion.repeatLastMatchChar(editor, caret, operatorArguments.count1.let { if (reverse) -it else it })
    return if (repeatLastMatchChar < 0) Motion.Error else Motion.AbsoluteOffset(repeatLastMatchChar)
  }

  fun MotionType.reverse(): MotionType {
    return when (this) {
      MotionType.INCLUSIVE -> MotionType.EXCLUSIVE
      MotionType.EXCLUSIVE -> MotionType.INCLUSIVE
      else -> throw IllegalArgumentException("no reverse for $this")
    }
  }
}
