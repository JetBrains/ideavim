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
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :@"
 */
data class RepeatCommand(val ranges: Ranges, val argument: String) : Command.ForEachCaret(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.SELF_SYNCHRONIZED)

  private var lastArg = ':'

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments
  ): ExecutionResult {
    var arg = argument[0]
    if (arg == '@') arg = lastArg
    lastArg = arg

    val line = getLine(editor, caret)
    injector.motion.moveCaret(
      editor,
      caret,
      injector.motion.moveCaretToLineWithSameColumn(editor, line, editor.primaryCaret())
    )

    if (arg == ':') {
      return if (injector.vimscriptExecutor.executeLastCommand(editor, context)) ExecutionResult.Success else ExecutionResult.Error
    }

    val reg = injector.registerGroup.getPlaybackRegister(arg) ?: return ExecutionResult.Error
    val text = reg.text ?: return ExecutionResult.Error

    injector.vimscriptExecutor.execute(text, editor, context, skipHistory = false, indicateErrors = true, this.vimContext)
    return ExecutionResult.Success
  }
}
