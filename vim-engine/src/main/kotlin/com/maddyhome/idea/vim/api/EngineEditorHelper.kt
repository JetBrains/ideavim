package com.maddyhome.idea.vim.api

interface EngineEditorHelper {
  fun normalizeOffset(editor: VimEditor, offset: Int, allowEnd: Boolean): Int
}