package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService

/**
 * see :h lockvar
 */
class LockVarCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // todo doesn't throw proper vim exceptions in case of wrong arguments
  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    val variableAndDepth = parseVariableAndDepth(argument)
    VariableService.lockVariable(variableAndDepth.first, variableAndDepth.second, editor, context, parent)
    return ExecutionResult.Success
  }
}

/**
 * see :h unlockvar
 */
class UnlockVarCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // todo doesn't throw proper vim exceptions in case of wrong arguments
  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    val variableAndDepth = parseVariableAndDepth(argument)
    VariableService.unlockVariable(variableAndDepth.first, variableAndDepth.second, editor, context, parent)
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
