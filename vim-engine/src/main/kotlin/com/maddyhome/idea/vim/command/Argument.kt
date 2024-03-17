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
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import java.util.*

/**
 * This represents a command argument.
 */
public class Argument private constructor(
  public val character: Char = 0.toChar(),
  public val motion: Command = EMPTY_COMMAND,
  public val offsets: Map<ImmutableVimCaret, VimSelection> = emptyMap(),
  public val string: String = "",
  public val type: Type,
) {
  public constructor(motionArg: Command) : this(motion = motionArg, type = Type.MOTION)
  public constructor(charArg: Char) : this(character = charArg, type = Type.CHARACTER)
  public constructor(strArg: String) : this(string = strArg, type = Type.EX_STRING)
  public constructor(offsets: Map<ImmutableVimCaret, VimSelection>) : this(offsets = offsets, type = Type.OFFSETS)

  public enum class Type {
    MOTION, CHARACTER, DIGRAPH, EX_STRING, OFFSETS
  }

  public companion object {
    @JvmField
    public val EMPTY_COMMAND: Command = Command(
      0,
      object : MotionActionHandler.SingleExecution() {
        override fun getOffset(
          editor: VimEditor,
          context: ExecutionContext,
          argument: Argument?,
          operatorArguments: OperatorArguments,
        ) = Motion.NoMotion

        override val motionType: MotionType = MotionType.EXCLUSIVE
      },
      Command.Type.MOTION,
      EnumSet.noneOf(CommandFlags::class.java),
    )
  }
}
