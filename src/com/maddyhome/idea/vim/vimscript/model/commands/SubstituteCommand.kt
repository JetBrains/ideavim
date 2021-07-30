package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator

// todo
data class SubstituteCommand(
  val ranges: Ranges,
  val variable: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
  val commandString: String,
) : Command.SingleExecution(ranges, commandString) {

  override val argFlags = CommandHandlerFlags(RangeFlag.RANGE_OPTIONAL, Access.SELF_SYNCHRONIZED, emptySet())

  override fun processCommand(editor: Editor?, context: DataContext?, vimContext: VimContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
//    if (editor != null) {
//      for (caret in editor.caretModel.allCarets) {
//        val lineRange = this.getLineRange(editor, caret)
//        if (!VimPlugin.getSearch().processSubstituteCommand(editor, caret, lineRange, cmd.command, cmd.argument)) {
//          result = ExecutionResult.Error
//        }
//      }
//    } else {
//      TODO()
//    }
    return result
  }
}
