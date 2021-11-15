package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

interface VariableService {

  fun isVariableLocked(variable: Variable, editor: Editor, context: DataContext, parent: Executable): Boolean

  fun lockVariable(variable: Variable, depth: Int, editor: Editor, context: DataContext, parent: Executable)

  fun unlockVariable(variable: Variable, depth: Int, editor: Editor, context: DataContext, parent: Executable)

  fun storeVariable(variable: Variable, value: VimDataType, editor: Editor, context: DataContext, parent: Executable)

  // todo replace with one method after Result class
  fun getGlobalVariableValue(name: String): VimDataType?
  fun getNullableVariableValue(variable: Variable, editor: Editor, context: DataContext, parent: Executable): VimDataType?
  fun getNonNullVariableValue(variable: Variable, editor: Editor, context: DataContext, parent: Executable): VimDataType
}
