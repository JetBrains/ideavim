/*
 * Copyright 2003-2022 The IdeaVim authors
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

  // TODO: 10.02.2022 Not sure about this method
  fun updateEditor(editor: VimEditor): ExecutionContext
}

interface ExecutionContextManager {
  fun onEditor(editor: VimEditor, prevContext: ExecutionContext? = null): ExecutionContext
  fun onCaret(caret: VimCaret, prevContext: ExecutionContext): ExecutionContext
  fun createCaretSpecificDataContext(context: ExecutionContext, caret: VimCaret): ExecutionContext
  fun createEditorDataContext(editor: VimEditor, context: ExecutionContext): ExecutionContext
}
