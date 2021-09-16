package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

data class FunctionCallExpression(val scope: Scope?, val functionName: String, val arguments: List<Expression>) :
  Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    val handler = FunctionStorage.getFunctionHandler(functionName, scope = scope, parent)
    return handler.executeFunction(this, editor, context, parent)
  }
}
