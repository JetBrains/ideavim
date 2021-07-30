package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator

data class BinExpression(val left: Expression, val right: Expression, val operator: BinaryOperator) : Expression() {

  override fun evaluate(editor: Editor?, context: DataContext?, vimContext: VimContext): VimDataType {
    return operator.handler.performOperation(
      left.evaluate(editor, context, vimContext),
      right.evaluate(editor, context, vimContext)
    )
  }
}
