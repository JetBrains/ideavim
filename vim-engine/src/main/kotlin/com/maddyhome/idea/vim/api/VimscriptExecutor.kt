package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import java.io.File

interface VimscriptExecutor {

  var executingVimscript: Boolean

  fun execute(script: String, editor: VimEditor, context: ExecutionContext, skipHistory: Boolean, indicateErrors: Boolean = true, vimContext: VimLContext? = null): ExecutionResult

  fun execute(script: String, skipHistory: Boolean = true)

  fun executeFile(file: File, indicateErrors: Boolean = false)

  fun executeLastCommand(editor: VimEditor, context: ExecutionContext): Boolean
}
