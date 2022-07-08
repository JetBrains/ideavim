package com.maddyhome.idea.vim.api

interface VimLookupManager {
  fun getActiveLookup(editor: VimEditor): IdeLookup?
}

interface IdeLookup {
  fun down(caret: VimCaret, context: ExecutionContext)
  fun up(caret: VimCaret, context: ExecutionContext)
}
