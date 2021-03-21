/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys

class RegistersHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {

    val registerGroup = VimPlugin.getRegister()
    val regs = registerGroup.registers
      .filter { cmd.argument.isEmpty() || cmd.argument.contains(it.name) }
      .joinToString("\n", prefix = "Type Name Content\n") { reg ->
        val type = when (reg.type) {
          SelectionType.LINE_WISE -> "l"
          SelectionType.CHARACTER_WISE -> "c"
          SelectionType.BLOCK_WISE -> "b"
        }
        val text = reg.rawText?.let { parseKeys(it) } ?: reg.keys
        "  $type  \"${reg.name}   ${StringHelper.toPrintableCharacters(text).take(200)}"
      }

    ExOutputModel.getInstance(editor).output(regs)

    return true
  }
}
