/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.util.*

sealed class Argument {
  class Character(val character: Char) : Argument()
  class ExString(val label: Char, val string: String, val processing: ((String) -> Unit)?) : Argument()

  /**
   * Base type for motion arguments
   */
  class Motion(val motion: Command) : Argument() {
    // TODO: Should this be MotionType?
    fun getMotionType() = if (motion.isLinewiseMotion()) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE
  }

  enum class Type {
    MOTION, CHARACTER, DIGRAPH
  }

  companion object {
    // TODO: Can we get rid of this?
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
