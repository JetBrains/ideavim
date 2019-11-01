/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VimActionHandler
import java.util.*

/**
 * This represents a command argument.
 */
class Argument private constructor(
  val character: Char = 0.toChar(),
  val motion: Command = EMPTY_COMMAND,
  val offsets: Map<Caret, VimSelection> = emptyMap(),
  val string: String = "",
  val type: Type
) {
  constructor(motionArg: Command) : this(motion = motionArg, type = Type.MOTION)
  constructor(charArg: Char) : this(character = charArg, type = Type.CHARACTER)
  constructor(strArg: String) : this(string = strArg, type = Type.EX_STRING)
  constructor(offsets: Map<Caret, VimSelection>) : this(offsets = offsets, type = Type.OFFSETS)

  enum class Type {
    MOTION, CHARACTER, DIGRAPH, EX_STRING, OFFSETS
  }

  companion object {
    @JvmField
    val EMPTY_COMMAND = Command(0, object : VimActionHandler.SingleExecution() {
      override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean = true

      override val type: Command.Type = Command.Type.UNDEFINED
    }, Command.Type.UNDEFINED, EnumSet.noneOf(CommandFlags::class.java), emptyList())
  }
}
