package com.maddyhome.idea.vim.undo

import com.maddyhome.idea.vim.api.ExecutionContext

interface VimUndoRedo {
  fun undo(context: ExecutionContext): Boolean
  fun redo(context: ExecutionContext): Boolean
}
