/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import java.util.*

/**
 * This represents a command argument.
 */
class Argument private constructor(
  val character: Char = 0.toChar(),
  val motion: Command = EMPTY_COMMAND,
  val offsets: Map<VimCaret, VimSelection> = emptyMap(),
  val string: String = "",
  val type: Type,
) {
  constructor(motionArg: Command) : this(motion = motionArg, type = Type.MOTION)
  constructor(charArg: Char) : this(character = charArg, type = Type.CHARACTER)
  constructor(strArg: String) : this(string = strArg, type = Type.EX_STRING)
  constructor(offsets: Map<VimCaret, VimSelection>) : this(offsets = offsets, type = Type.OFFSETS)

  enum class Type {
    MOTION, CHARACTER, DIGRAPH, EX_STRING, OFFSETS
  }

  companion object {
    @JvmField
    val EMPTY_COMMAND = Command(
      0,
      object : MotionActionHandler.SingleExecution() {
        override fun getOffset(
          editor: VimEditor,
          context: ExecutionContext,
          argument: Argument?,
          operatorArguments: OperatorArguments
        ) = Motion.NoMotion

        override val motionType: MotionType = MotionType.EXCLUSIVE
      },
      Command.Type.MOTION, EnumSet.noneOf(CommandFlags::class.java)
    )
  }
}
