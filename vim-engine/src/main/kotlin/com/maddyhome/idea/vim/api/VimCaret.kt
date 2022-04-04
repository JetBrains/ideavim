package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.Offset

// TODO: 29.12.2021 Split interface to mutable and immutable
interface VimCaret {
  val editor: VimEditor
  val offset: Offset
  var vimLastColumn: Int
  val selectionStart: Int
  val selectionEnd: Int
  var vimSelectionStart: Int
  val vimLeadSelectionOffset: Int
  fun moveToOffset(offset: Int)
  fun offsetForLineStartSkipLeading(line: Int): Int
  fun getLine(): EditorLine.Pointer
  fun hasSelection(): Boolean
  fun vimSetSystemSelectionSilently(start: Int, end: Int)
  val isValid: Boolean
  fun moveToInlayAwareOffset(newOffset: Int)
  fun vimSetSelection(start: Int, end: Int = start, moveCaretToSelectionEnd: Boolean = false)
  fun getVisualPosition(): VimVisualPosition
  val visualLineStart: Int
}