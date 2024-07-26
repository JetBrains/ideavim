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
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError

enum class TillCharacterMotionType {
  LAST_F,
  LAST_SMALL_F,
  LAST_T,
  LAST_SMALL_T,
}

@CommandOrMotion(keys = ["F"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionLeftMatchCharAction : TillCharacterMotion(Direction.BACKWARDS, TillCharacterMotionType.LAST_F, false)

@CommandOrMotion(keys = ["T"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionLeftTillMatchCharAction : TillCharacterMotion(Direction.BACKWARDS, TillCharacterMotionType.LAST_T, true)

@CommandOrMotion(keys = ["f"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionRightMatchCharAction : TillCharacterMotion(Direction.FORWARDS, TillCharacterMotionType.LAST_SMALL_F, false)

@CommandOrMotion(keys = ["t"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionRightTillMatchCharAction :
  TillCharacterMotion(Direction.FORWARDS, TillCharacterMotionType.LAST_SMALL_T, true)

sealed class TillCharacterMotion(
  private val direction: Direction,
  private val tillCharacterMotionType: TillCharacterMotionType,
  private val finishBeforeCharacter: Boolean,
) : MotionActionHandler.ForEachCaret() {
  override val argumentType: Argument.Type = Argument.Type.DIGRAPH
  override val motionType: MotionType =
    if (direction == Direction.BACKWARDS) MotionType.EXCLUSIVE else MotionType.INCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    if (argument !is Argument.Character) return Motion.Error
    val res = if (finishBeforeCharacter) {
      injector.motion
        .moveCaretToBeforeNextCharacterOnLine(
          editor,
          caret,
          direction.toInt() * operatorArguments.count1,
          argument.character,
        )
    } else {
      injector.motion.moveCaretToNextCharacterOnLine(
        editor,
        caret,
        direction.toInt() * operatorArguments.count1,
        argument.character,
      )
    }
    injector.motion.setLastFTCmd(tillCharacterMotionType, argument.character)
    return res.toMotionOrError()
  }
}
