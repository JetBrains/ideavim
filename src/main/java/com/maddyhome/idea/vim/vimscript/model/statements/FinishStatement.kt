package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

object FinishStatement : Executable {
  override lateinit var parent: Executable

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    throw FinishException()
  }
}
