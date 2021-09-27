package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

/**
 * see "h :echo"
 */
data class EchoCommand(val ranges: Ranges, val args: List<Expression>) :
  Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult.Success {
    val text = args.joinToString(separator = " ", postfix = "\n") {
      it.evaluate(editor, context, vimContext).toString()
    }
    ExOutputModel.getInstance(editor).output(text)
    return ExecutionResult.Success
  }
}
