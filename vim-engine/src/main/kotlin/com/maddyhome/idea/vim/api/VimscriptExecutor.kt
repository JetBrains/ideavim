/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import java.io.File

public interface VimscriptExecutor {

  public var executingVimscript: Boolean

  public fun execute(script: String, editor: VimEditor, context: ExecutionContext, skipHistory: Boolean, indicateErrors: Boolean = true, vimContext: VimLContext? = null): ExecutionResult

  public fun executeFile(file: File, editor: VimEditor, indicateErrors: Boolean = false)

  public fun executeLastCommand(editor: VimEditor, context: ExecutionContext): Boolean
}
