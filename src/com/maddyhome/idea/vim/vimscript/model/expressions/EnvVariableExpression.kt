package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class EnvVariableExpression(val variableName: String) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, vimContext: VimContext): VimDataType {
    TODO("Not yet implemented")
  }
}
