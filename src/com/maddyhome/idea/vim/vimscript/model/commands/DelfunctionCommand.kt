package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

/**
 * see "h :delfunction"
 */
data class DelfunctionCommand(
  val ranges: Ranges,
  val scope: Scope?,
  val name: String,
  val ignoreIfMissing: Boolean,
) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    if (ignoreIfMissing) {
      try {
        FunctionStorage.deleteFunction(name, scope, this)
      } catch (e: ExException) {
        if (e.message != null && e.message!!.startsWith("E130")) {
          // "ignoreIfMissing" flag handles the "E130: Unknown function" exception
        } else {
          throw e
        }
      }
    } else {
      FunctionStorage.deleteFunction(name, scope, this)
    }
    return ExecutionResult.Success
  }
}
