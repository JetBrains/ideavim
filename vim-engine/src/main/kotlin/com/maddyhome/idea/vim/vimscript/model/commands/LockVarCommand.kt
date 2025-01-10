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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

/**
 * see :h lockvar
 */
@ExCommand(command = "lockv[ar]")
class LockVarCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // todo doesn't throw proper vim exceptions in case of wrong arguments
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val variableAndDepth = parseVariableAndDepth(modifier, argument)
    injector.variableService.lockVariable(variableAndDepth.first, variableAndDepth.second, editor, context, vimContext)
    return ExecutionResult.Success
  }
}

/**
 * see :h unlockvar
 */
@ExCommand(command = "unlo[ckvar]")
class UnlockVarCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // todo doesn't throw proper vim exceptions in case of wrong arguments
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val variableAndDepth = parseVariableAndDepth(modifier, argument)
    injector.variableService.unlockVariable(
      variableAndDepth.first,
      variableAndDepth.second,
      editor,
      context,
      vimContext
    )
    return ExecutionResult.Success
  }
}

private fun parseVariableAndDepth(modifier: CommandModifier, argument: String): Pair<Variable, Int> {
  val variable: String
  var depth = if (modifier == CommandModifier.BANG) 100 else 2
  val args = argument.trim().split(" ")
  when (args.size) {
    1 -> variable = args[0]
    2 -> {
      depth = args[0].toIntOrNull() ?: 2
      variable = args[1]
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
