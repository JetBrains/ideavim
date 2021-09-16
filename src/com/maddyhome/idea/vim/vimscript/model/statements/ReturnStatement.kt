package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

data class ReturnStatement(val expression: Expression) : Executable() {

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    return ExecutionResult.Return(expression.evaluate(editor, context, this))
  }
}
