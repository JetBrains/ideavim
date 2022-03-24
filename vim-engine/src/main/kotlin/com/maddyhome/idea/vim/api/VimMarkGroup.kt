package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

interface VimMarkGroup {
  fun saveJumpLocation(editor: VimEditor)
  fun setChangeMarks(vimEditor: VimEditor, range: TextRange)
}
