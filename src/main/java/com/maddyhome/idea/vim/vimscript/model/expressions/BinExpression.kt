package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator

data class BinExpression(val left: Expression, val right: Expression, val operator: BinaryOperator) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    return operator.handler.performOperation(
      left.evaluate(editor, context, parent),
      right.evaluate(editor, context, parent)
    )
  }
}
