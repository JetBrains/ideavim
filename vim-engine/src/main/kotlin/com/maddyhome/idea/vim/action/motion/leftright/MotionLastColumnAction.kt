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
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.state.mode.inVisualMode
import java.util.*

abstract class MotionLastColumnBaseAction(private val isMotionForOperator: Boolean = false) :
  MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.INCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val allowPastEnd = if (editor.inVisualMode) {
      injector.options(editor).selection != "old"
    } else {
      // Don't allow past end if this motion is for an operator. I.e., for something like `d$`, we don't want to delete
      // the end of line character
      if (isMotionForOperator) false else editor.isEndAllowed
    }

    val offset = injector.motion.moveCaretToRelativeLineEnd(editor, caret, operatorArguments.count1 - 1, allowPastEnd)
    return Motion.AdjustedOffset(offset, VimMotionGroupBase.LAST_COLUMN)
  }
}

@CommandOrMotion(keys = ["$"], modes = [Mode.NORMAL, Mode.VISUAL])
open class MotionLastColumnAction : MotionLastColumnBaseAction()

@CommandOrMotion(keys = ["$"], modes = [Mode.OP_PENDING])
class MotionLastColumnOpPendingAction : MotionLastColumnBaseAction(isMotionForOperator = true)

@CommandOrMotion(keys = ["<End>"], modes = [Mode.INSERT])
class MotionLastColumnInsertAction : MotionLastColumnAction() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}
