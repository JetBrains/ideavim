package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class ScopeExpression(val scope: Scope) : Expression() {

  // this expression should never have been evaluated and only be used as a function argument
  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    throw ExException("Cannot evaluate scope expression: $scope")
  }
}
