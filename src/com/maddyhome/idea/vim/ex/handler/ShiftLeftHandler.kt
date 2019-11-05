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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandlerFlags
import com.maddyhome.idea.vim.ex.CommandName
import com.maddyhome.idea.vim.ex.ComplicatedNameExCommand
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags

class ShiftLeftHandler : CommandHandler.ForEachCaret(), ComplicatedNameExCommand {
  override val names: Array<CommandName> = commands("<[${"<".repeat(31)}]")
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: ExCommand): Boolean {
    val range = cmd.getTextRange(editor, caret, context, true)
    val endOffsets = range.endOffsets.map { it - 1 }.toIntArray()
    VimPlugin.getChange().indentRange(editor, caret, context,
      TextRange(range.startOffsets, endOffsets),
      cmd.command.length, -1)
    return true
  }
}
