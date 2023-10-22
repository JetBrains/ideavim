/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
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
@ExCommand(command = "action")
public data class ActionCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges) {

  override val argFlags: CommandHandlerFlags = flags(
    RangeFlag.RANGE_OPTIONAL,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.READ_ONLY,
    Flag.SAVE_VISUAL,
  )

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val actionName = argument.trim()
    val action = injector.actionExecutor.getAction(actionName)
      ?: throw ExException(injector.messages.message("action.not.found.0", actionName))
    if (injector.application.isUnitTest()) {
      executeAction(editor, action, context)
    } else {
      injector.application.runAfterGotFocus { executeAction(editor, action, context) }
    }
    return ExecutionResult.Success
  }

  private fun executeAction(editor: VimEditor, action: NativeAction, context: ExecutionContext) {
    injector.actionExecutor.executeAction(editor, action, context)
  }
}
