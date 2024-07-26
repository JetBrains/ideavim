/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.util.*

sealed class Argument {
  class Character(val character: Char) : Argument()
  class ExString(val label: Char, val string: String, val processing: ((String) -> Unit)?) : Argument()

  /**
   * Base type for motion arguments
   */
  abstract class Motion() : Argument() {
    open fun getMotionType() = SelectionType.CHARACTER_WISE
  }

  /**
   * A traditional Vim-like motion or text object
   */
  class MotionAction(val motion: Command) : Motion() {
    override fun getMotionType() =
      if (motion.isLinewiseMotion()) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE
  }

  /**
   * Represents a motion as a set of offset destinations
   *
   * This is used to wrap external motion functionality and allow it to be used as an argument to an operator. For
   * example, the AceJump plugin can provide a motion which will move the caret(s), and this argument will capture the
   * resulting offset destination(s) of the caret(s).
   *
   * TODO: Consider wrapping this in an action
   * Having two methods of motion (Vim actions and externally calculated offsets) complicates implementations because
   * all operators (or helpers) need to handle both in order to get offsets. The closest match would be
   * TextObjectActionHandler
   */
  class Offsets(val offsets: Map<ImmutableVimCaret, VimSelection>) : Motion()

  enum class Type {
    MOTION, CHARACTER, DIGRAPH
  }

  companion object {
    @JvmField
    val EMPTY_COMMAND: Command = Command(
      0,
      object : MotionActionHandler.SingleExecution() {
        override fun getOffset(
          editor: VimEditor,
          context: ExecutionContext,
          argument: Argument?,
          operatorArguments: OperatorArguments,
        ) = com.maddyhome.idea.vim.handler.Motion.NoMotion

        override val motionType: MotionType = MotionType.EXCLUSIVE
      },
      Command.Type.MOTION,
      EnumSet.noneOf(CommandFlags::class.java),
    )
  }
}
