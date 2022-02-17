package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.newapi.VimEditor

// TODO: 29.12.2021 Split interface to mutable and immutable
interface VimCaret {
  val editor: VimEditor
  val offset: Offset
  fun moveToOffset(offset: Int)
  fun offsetForLineStartSkipLeading(line: Int): Int
  fun getLine(): EditorLine.Pointer
  fun hasSelection(): Boolean
}