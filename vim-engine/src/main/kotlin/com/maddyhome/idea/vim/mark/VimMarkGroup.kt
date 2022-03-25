package com.maddyhome.idea.vim.mark

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange

interface VimMarkGroup {
  fun saveJumpLocation(editor: VimEditor)
  fun setChangeMarks(vimEditor: VimEditor, range: TextRange)
}
