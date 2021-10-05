package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class FunctionCallExpression(val scope: Scope?, val functionName: String, val arguments: MutableList<Expression>) :
  Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    val handler = FunctionStorage.getFunctionHandlerOrNull(scope, functionName, parent)
    if (handler != null) {
      if (handler is DefinedFunctionHandler && handler.function.flags.contains(FunctionFlag.DICT)) {
        throw ExException("E725: Calling dict function without Dictionary: ${(if (scope != null) scope.c + ":" else "") + functionName}")
      }
      return handler.executeFunction(this.arguments, editor, context, parent)
    }

    val funcref = VariableService.getNullableVariableValue(Variable(scope, functionName), editor, context, parent)
    if (funcref is VimFuncref) {
      if (funcref.handler is DefinedFunctionHandler && funcref.handler.function.flags.contains(FunctionFlag.DICT) && funcref.handler.function.self == null) {
        throw ExException("E725: Calling dict function without Dictionary: ${(if (scope != null) scope.c + ":" else "") + functionName}")
      }
      return funcref.execute(arguments, editor, context, parent)
    }
    throw ExException("E117: Unknown function: ${if (scope != null) scope.c + ":" else ""}$functionName")
  }
}
