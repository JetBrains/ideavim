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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.vimscript.Executor
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext

data class RepeatCommand(val ranges: Ranges, val argument: String) : Command.ForEachCaret(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.SELF_SYNCHRONIZED)

  private var lastArg = ':'

  @Throws(ExException::class)
  override fun processCommand(editor: Editor, caret: Caret, context: DataContext, vimContext: VimContext): ExecutionResult {
    var arg = argument[0]
    if (arg == '@') arg = lastArg
    lastArg = arg

    val line = getLine(editor, caret)
    MotionGroup.moveCaret(
      editor,
      caret,
      VimPlugin.getMotion().moveCaretToLineWithSameColumn(editor, line, editor.caretModel.primaryCaret)
    )

    if (arg == ':') {
      return if (Executor.executeLastCommand(editor, context)) ExecutionResult.Success else ExecutionResult.Error
    }

    val reg = VimPlugin.getRegister().getPlaybackRegister(arg) ?: return ExecutionResult.Error
    val text = reg.text ?: return ExecutionResult.Error

    Executor.execute(text, editor, context, false)
    return ExecutionResult.Success
  }
}
