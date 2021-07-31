package com.maddyhome.idea.vim.vimscript.model.statements.loops

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

data class WhileLoop(val condition: Expression, val body: List<Executable>) : Executable {

  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    while (condition.evaluate(editor, context, vimContext).asBoolean()) {
      for (statement in body) {
        if (result is ExecutionResult.Success) {
          result = statement.execute(editor, context, vimContext)
        } else {
          break
        }
      }
      if (result is ExecutionResult.Break) {
        result = ExecutionResult.Success
        break
      } else if (result is ExecutionResult.Continue) {
        result = ExecutionResult.Success
        continue
      }
    }
    return result
  }
}
