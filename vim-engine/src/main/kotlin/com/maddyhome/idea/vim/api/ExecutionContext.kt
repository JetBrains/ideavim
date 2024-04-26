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

public sealed interface ExecutionContext {
  public val context: Any

  // TODO: 10.02.2022 Not sure about this method
  public fun updateEditor(editor: VimEditor): ExecutionContext

  public interface Editor : ExecutionContext
}

public interface ExecutionContextManager {
  public fun onEditor(editor: VimEditor, prevContext: ExecutionContext? = null): ExecutionContext.Editor
}
