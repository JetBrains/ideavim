/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

/**
 * see :h lockvar
 */
class LockVarCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // todo doesn't throw proper vim exceptions in case of wrong arguments
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val variableAndDepth = parseVariableAndDepth(argument)
    injector.variableService.lockVariable(variableAndDepth.first, variableAndDepth.second, editor, context, vimContext)
    return ExecutionResult.Success
  }
}

/**
 * see :h unlockvar
 */
class UnlockVarCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // todo doesn't throw proper vim exceptions in case of wrong arguments
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val variableAndDepth = parseVariableAndDepth(argument)
    injector.variableService.unlockVariable(variableAndDepth.first, variableAndDepth.second, editor, context, vimContext)
    return ExecutionResult.Success
  }
}

private fun parseVariableAndDepth(argument: String): Pair<Variable, Int> {
  val variable: String
  var arg = argument
  var depth = 2
  if (arg.startsWith("!")) {
    depth = 100
    arg = arg.substring(1)
  }
  val splittedArg = arg.trim().split(" ")
  when (splittedArg.size) {
    1 -> variable = splittedArg[0]
    2 -> {
      depth = splittedArg[0].toIntOrNull() ?: 2
      variable = splittedArg[1]
    }
    else -> throw ExException("Unknown error during lockvar command execution")
  }
  return Pair(parseVariable(variable), depth)
}

private fun parseVariable(variable: String): Variable {
  val splittedString = variable.split(":")
  return when (splittedString.size) {
    1 -> Variable(null, splittedString[0])
    2 -> Variable(Scope.getByValue(splittedString[0]), splittedString[1])
    else -> throw ExException("Unknown error during lockvar command execution")
  }
}
