package com.maddyhome.idea.vim.vimscript.model

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

data class Script(val units: List<Executable>) : Executable {
  override lateinit var parent: Executable

  /**
   * we store the "s:" scope variables and functions here
   * see ":h scope"
   */
  val scriptVariables: MutableMap<String, VimDataType> = mutableMapOf()
  val scriptFunctions: MutableMap<String, FunctionHandler> = mutableMapOf()

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    var latestResult: ExecutionResult = ExecutionResult.Success
    for (unit in units) {
      unit.parent = this
      if (latestResult is ExecutionResult.Success) {
        latestResult = unit.execute(editor, context)
      } else {
        break
      }
    }
    return latestResult
  }
}
