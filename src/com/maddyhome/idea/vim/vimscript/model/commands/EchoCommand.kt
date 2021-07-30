package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

class EchoCommand(val ranges: Ranges, val args: List<Expression>, val commandString: String) :
  Command.SingleExecution(ranges, commandString) {

  override val argFlags = CommandHandlerFlags(RangeFlag.RANGE_FORBIDDEN, Access.READ_ONLY, emptySet())

  override fun processCommand(editor: Editor?, context: DataContext?, vimContext: VimContext): ExecutionResult.Success {
    if (editor != null) {
      val text = args.joinToString(separator = " ", postfix = "\n") {
        it.evaluate(editor, context, vimContext).toString()
      }
      ExOutputModel.getInstance(editor).output(text)
    }
    return ExecutionResult.Success
  }
}
