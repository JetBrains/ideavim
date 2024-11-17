/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.undo

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor

sealed interface VimUndoRedo {
  fun undo(editor: VimEditor, context: ExecutionContext): Boolean
  fun redo(editor: VimEditor, context: ExecutionContext): Boolean
}
