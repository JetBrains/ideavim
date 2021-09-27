package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class TernaryExpression(val condition: Expression, val then: Expression, val otherwise: Expression) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, vimContext: VimContext): VimDataType {
    return if (condition.evaluate(editor, context, vimContext).asDouble() != 0.0) {
      then.evaluate(editor, context, vimContext)
    } else {
      otherwise.evaluate(editor, context, vimContext)
    }
  }
}
