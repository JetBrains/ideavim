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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.NamedFunctionCallExpression

/**
 * see "h :call"
 */
@ExCommand(command = "cal[l]")
class CallCommand(val range: Range, val functionCall: Expression) :
  Command.SingleExecution(range, CommandModifier.NONE) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    when (functionCall) {
      is NamedFunctionCallExpression -> functionCall.evaluateWithRange(range, editor, context, vimContext)
      is FuncrefCallExpression -> functionCall.evaluateWithRange(range, editor, context, vimContext)

      else -> {
        // todo add more exceptions
        throw exExceptionMessage("E129")
      }
    }

    return ExecutionResult.Success
  }
}
