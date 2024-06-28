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

interface VimscriptExecutor {

  /**
   * True if Vimscript is under execution. This might be reading of .ideavimrc file, some :source command,
   *   command from the ex-command line, or any other case.
   */
  var executingVimscript: Boolean

  /**
   * This variable is set to true when we execute .ideavimrc configuration. This might be _ideavimrc file on windows
   *   or the file from XGD config directory, according to the settings.
   */
  var executingIdeaVimRcConfiguration: Boolean

  fun execute(
    script: String,
    editor: VimEditor,
    context: ExecutionContext,
    skipHistory: Boolean,
    indicateErrors: Boolean = true,
    vimContext: VimLContext? = null,
  ): ExecutionResult

  fun executeFile(
    file: File,
    editor: VimEditor,
    fileIsIdeaVimRcConfig: Boolean,
    indicateErrors: Boolean = false,
  )

  fun executeLastCommand(editor: VimEditor, context: ExecutionContext): Boolean
}
