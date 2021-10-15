package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class Variable(val scope: Scope?, val name: CurlyBracesName) : Expression() {
  constructor(scope: Scope?, name: String) : this(scope, CurlyBracesName(listOf(SimpleExpression(name))))

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    return VariableService.getNonNullVariableValue(this, editor, context, parent)
  }

  fun toString(editor: Editor, context: DataContext, parent: Executable): String {
    return (scope?.toString() ?: "") + name.evaluate(editor, context, parent)
  }
}
