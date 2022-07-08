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
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author smartbomb
 */
data class ActionCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges) {

  override val argFlags = flags(
    RangeFlag.RANGE_OPTIONAL,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.READ_ONLY,
    Flag.SAVE_VISUAL
  )

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val actionName = argument.trim()
    val action = injector.actionExecutor.getAction(actionName) ?: throw ExException(injector.messages.message("action.not.found.0", actionName))
    if (injector.application.isUnitTest()) {
      executeAction(action, context)
    } else {
      injector.application.runAfterGotFocus { executeAction(action, context) }
    }
    return ExecutionResult.Success
  }

  private fun executeAction(action: NativeAction, context: ExecutionContext) {
    injector.actionExecutor.executeAction(action, context)
  }
}
