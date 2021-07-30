package com.maddyhome.idea.vim.vimscript.model

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor

interface Executable {

  fun execute(editor: Editor?, context: DataContext?, vimContext: VimContext, skipHistory: Boolean = true): ExecutionResult
}
