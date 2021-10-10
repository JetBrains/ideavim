package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class TernaryExpression(val condition: Expression, val then: Expression, val otherwise: Expression) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    return if (condition.evaluate(editor, context, parent).asDouble() != 0.0) {
      then.evaluate(editor, context, parent)
    } else {
      otherwise.evaluate(editor, context, parent)
    }
  }
}
