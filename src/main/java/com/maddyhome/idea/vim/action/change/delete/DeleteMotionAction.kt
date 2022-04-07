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
package com.maddyhome.idea.vim.action.change.delete

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.helper.experimentalApi
import com.maddyhome.idea.vim.newapi.deleteRange

class DeleteMotionAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.DELETE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = 'd'

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (argument == null) return false
    if (experimentalApi()) {
      val (first, second) = VimPlugin.getChange()
        .getDeleteRangeAndType2(editor, caret, context, argument, false, operatorArguments)
        ?: return false
      return deleteRange(editor, caret, first, second)
    } else {
      val (first, second) = VimPlugin.getChange()
        .getDeleteRangeAndType(editor, caret, context, argument, false, operatorArguments)
        ?: return false
      return VimPlugin.getChange().deleteRange(editor, caret, first, second, false)
    }
  }
}
