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

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext

/**
 * @author smartbomb
 */
data class ActionCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY, Flag.SAVE_VISUAL)

  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    val actionName = argument.trim()
    val action = ActionManager.getInstance().getAction(actionName) ?: throw ExException(MessageHelper.message("action.not.found.0", actionName))
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode) {
      executeAction(action, context)
    } else {
      runAfterGotFocus { executeAction(action, context) }
    }
    return ExecutionResult.Success
  }

  private fun executeAction(action: AnAction, context: DataContext) {
    KeyHandler.executeAction(action, context)
  }
}
