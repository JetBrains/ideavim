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
import java.util.*

/**
 * This represents a command argument.
 * TODO please make it a sealed class and not a giant collection of fields with default values, it's not safe
 */
sealed class Argument private constructor(
  val character: Char = 0.toChar(),
  val motion: Command = EMPTY_COMMAND,
  val offsets: Map<ImmutableVimCaret, VimSelection> = emptyMap(),
  val string: String = "",
  val processing: ((String) -> Unit)? = null,
  val type: Type,
) {
  class Motion(motion: Command) : Argument(motion = motion, type = Type.MOTION)
  class Character(character: Char) : Argument(character = character, type = Type.CHARACTER)
  class ExString(label: Char, strArg: String, processing: ((String) -> Unit)?) : Argument(character = label, string = strArg, processing = processing, type = Type.EX_STRING)
  class Offsets(offsets: Map<ImmutableVimCaret, VimSelection>) : Argument(offsets = offsets, type = Type.OFFSETS)

  enum class Type {
    MOTION, CHARACTER, DIGRAPH, EX_STRING, OFFSETS
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
