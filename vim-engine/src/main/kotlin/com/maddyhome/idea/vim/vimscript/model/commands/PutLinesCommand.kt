/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :put"
 */
data class PutLinesCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    if (editor.isOneLineMode()) return ExecutionResult.Error

    val registerGroup = injector.registerGroup
    val arg = argument
    if (arg.isNotEmpty()) {
      if (!registerGroup.selectRegister(arg[0]))
        return ExecutionResult.Error
    } else {
      registerGroup.selectRegister(registerGroup.defaultRegister)
    }

    val line = if (ranges.size() == 0) -1 else getLine(editor)
    val textData = registerGroup.lastRegister?.let {
      PutData.TextData(
        it.text ?: injector.parser.toKeyNotation(it.keys),
        SelectionType.LINE_WISE,
        it.transferableData
      )
    }
    val putData = PutData(
      textData,
      null,
      1,
      insertTextBeforeCaret = false,
      rawIndent = false,
      caretAfterInsertedText = false,
      putToLine = line
    )
    return if (injector.put.putText(editor, context, putData)) ExecutionResult.Success else ExecutionResult.Error
  }
}
