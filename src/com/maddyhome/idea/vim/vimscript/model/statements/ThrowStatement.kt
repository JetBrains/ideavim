package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

data class ThrowStatement(val expression: Expression) : Executable {
  override lateinit var parent: Executable

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    throw ExException(expression.evaluate(editor, context, this).toString())
  }
}
