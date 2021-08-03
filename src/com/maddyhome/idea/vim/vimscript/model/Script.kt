package com.maddyhome.idea.vim.vimscript.model

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor

data class Script(val units: List<Executable>) : Executable {

  override fun execute(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    var latestResult: ExecutionResult = ExecutionResult.Success
    for (unit in units) {
      if (latestResult is ExecutionResult.Success) {
        latestResult = unit.execute(editor, context, vimContext)
      } else {
        break
      }
    }
    return latestResult
  }
}
