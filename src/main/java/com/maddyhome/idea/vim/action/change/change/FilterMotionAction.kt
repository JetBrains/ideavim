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

package com.maddyhome.idea.vim.action.change.change

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.newapi.ij

class FilterMotionAction : VimActionHandler.SingleExecution(), DuplicableOperatorAction {

  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = '!'

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val argument = cmd.argument ?: return false
    val range = MotionGroup
      .getMotionRange(
        editor.ij, editor.primaryCaret().ij, context.ij,
        argument,
        operatorArguments
      )
      ?: return false

    val current = editor.ij.caretModel.logicalPosition
    val start = editor.offsetToLogicalPosition(range.startOffset)
    val end = editor.offsetToLogicalPosition(range.endOffsetInclusive)
    if (current.line != start.line) {
      MotionGroup.moveCaret(editor.ij, editor.primaryCaret().ij, range.startOffset)
    }

    val count = if (start.line < end.line) end.line - start.line + 1 else 1

    VimPlugin.getProcess().startFilterCommand(editor.ij, context.ij, Argument.EMPTY_COMMAND.copy(rawCount = count))

    return true
  }
}
