package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

data class FunctionCallExpression(val scope: Scope?, val functionName: String, val arguments: List<Expression>) :
  Expression() {

  override fun evaluate(editor: Editor, context: DataContext, vimContext: VimContext): VimDataType {
    val handler = FunctionStorage.getFunctionHandler(functionName, vimContext, scope = scope)
    return handler.executeFunction(this, editor, context, vimContext)
  }
}
