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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.LValueExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator

/**
 * see "h :let"
 */
@ExCommand(command = "let")
data class LetCommand(
  val range: Range,
  val lvalue: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
  val isSyntaxSupported: Boolean,
  val assignmentTextForErrors: String
) : Command.SingleExecution(range, CommandModifier.NONE) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (!isSyntaxSupported) return ExecutionResult.Error

    if (lvalue is LValueExpression) {
      val currentValue =
        if (operator != AssignmentOperator.ASSIGNMENT) lvalue.evaluate(editor, context, vimContext) else null
      val rhs = expression.evaluate(editor, context, vimContext)
      if (lvalue is SublistExpression && operator != AssignmentOperator.ASSIGNMENT && currentValue is VimList && rhs is VimList) {
        // Compound assigment operators modifies each item in a sublist expression, in-place, even if an error is
        // encountered. So we get a new sublist with all the changes up to the first error, assign it, then throw
        val result = getNewSublistValue(currentValue, rhs, lvalue.isStronglyTyped())
        lvalue.assign(result.first, editor, context, this, assignmentTextForErrors)
        result.second?.let { throw it }
      } else {
        val newValue = operator.getNewValue(currentValue, rhs, lvalue.isStronglyTyped())
        lvalue.assign(newValue, editor, context, this, assignmentTextForErrors)
      }
      return ExecutionResult.Success
    }

    // TODO: EnvVariableExpression

    throw exExceptionMessage("E121", lvalue.originalString)
  }

  /**
   * Get a new sublist value after applying a compound assignment operator to each item
   *
   * Compound assignments and lists are invalid. But compound assignment and sublist expressions will apply the operator
   * to each item in the sublist. Vim modifies the original list in-place, even if an error is encountered.
   *
   * Potentially modifying part of a list in-place is tricky with our default implementation, so we create an
   * intermediary result (sub)list that we can assign to the sublist expression. If any errors occur, we capture them so
   * they can be replayed once the partial result has been assigned.
   *
   * Note that we modify [sublist], because we know it's just been evaluated and won't be reused.
   */
  private fun getNewSublistValue(
    sublist: VimList,
    rhs: VimList,
    isLValueStronglyTyped: Boolean,
  ): Pair<VimList, ExException?> {
    var error: ExException? = null
    for (i in 0 until sublist.values.size) {
      if (i >= rhs.values.size) {
        error = exExceptionMessage("E711")
        break
      }
      sublist.values[i] = operator.getNewValue(sublist.values[i], rhs.values[i], isLValueStronglyTyped)
    }
    if (rhs.values.size > sublist.values.size) {
      error = exExceptionMessage("E710")
    }
    return Pair(sublist, error)
  }
}
