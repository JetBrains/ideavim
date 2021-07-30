package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.UnaryOperator

data class UnaryExpression(val operator: UnaryOperator, val expression: Expression) : Expression() {

  override fun evaluate(editor: Editor?, context: DataContext?, vimContext: VimContext): VimDataType {
    return operator.handler.performOperation(expression.evaluate(editor, context, vimContext))
  }
}
