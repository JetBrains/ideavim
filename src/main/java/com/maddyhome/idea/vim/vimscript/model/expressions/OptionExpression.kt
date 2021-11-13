package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.OptionService

data class OptionExpression(val scope: Scope, val optionName: String) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    return VimPlugin.getOptionService().getOptionValue(scope.toOptionScope(), optionName, editor)
  }
}

// todo clean me up
fun Scope.toOptionScope(): OptionService.Scope {
  return when (this) {
    Scope.GLOBAL_VARIABLE -> OptionService.Scope.GLOBAL
    Scope.LOCAL_VARIABLE -> OptionService.Scope.LOCAL
    else -> throw ExException("Invalid option scope")
  }
}
