package com.maddyhome.idea.vim.vimscript.model.statements.loops

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext

object BreakStatement : Executable {

  override fun execute(editor: Editor?, context: DataContext?, vimContext: VimContext): ExecutionResult {
    return ExecutionResult.Break
  }
}
