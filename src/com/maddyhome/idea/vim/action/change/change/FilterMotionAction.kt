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

package com.maddyhome.idea.vim.action.change.change

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.endOffsetInclusive


class FilterMotionAction : VimActionHandler.SingleExecution(), DuplicableOperatorAction {

  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = '!'

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val argument = cmd.argument ?: return false
    val range = MotionGroup
      .getMotionRange(editor, editor.caretModel.primaryCaret, context, cmd.count, cmd.rawCount,
        argument)
      ?: return false

    val current = editor.caretModel.logicalPosition
    val start = editor.offsetToLogicalPosition(range.startOffset)
    val end = editor.offsetToLogicalPosition(range.endOffsetInclusive)
    if (current.line != start.line) {
      MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, range.startOffset)
    }

    val count = if (start.line < end.line) end.line - start.line + 1 else 1

    VimPlugin.getProcess().startFilterCommand(editor, context, Argument.EMPTY_COMMAND.copy(rawCount = count))

    return true
  }
}

