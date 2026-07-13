/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimLookupManager {
  fun getActiveLookup(editor: VimEditor): IdeLookup?
  fun showCustomLookup(editor: VimEditor, values: List<String>)
}

interface IdeLookup {
  fun down(caret: ImmutableVimCaret, context: ExecutionContext)
  fun up(caret: ImmutableVimCaret, context: ExecutionContext)
  fun close(caret: ImmutableVimCaret, context: ExecutionContext)
  fun accept(caret: ImmutableVimCaret, context: ExecutionContext)
}
