/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * This would be ideal if we could provide a typed solution, but sofar this is just a holder
 */

interface ExecutionContext {
  val context: Any
}

interface ExecutionContextManager {
  fun getEditorExecutionContext(editor: VimEditor): ExecutionContext
}
