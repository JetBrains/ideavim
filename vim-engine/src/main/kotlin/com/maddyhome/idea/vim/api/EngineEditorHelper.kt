package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

interface EngineEditorHelper {
  fun normalizeOffset(editor: VimEditor, offset: Int, allowEnd: Boolean): Int
  fun getText(editor: VimEditor, range: TextRange): String
  fun getOffset(editor: VimEditor, line: Int, column: Int): Int
}