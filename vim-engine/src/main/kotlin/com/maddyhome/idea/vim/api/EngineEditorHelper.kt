package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

interface EngineEditorHelper {
  fun normalizeOffset(editor: VimEditor, offset: Int, allowEnd: Boolean): Int
  fun normalizeOffset(editor: VimEditor, line: Int, offset: Int, allowEnd: Boolean): Int
  fun getText(editor: VimEditor, range: TextRange): String
  fun getOffset(editor: VimEditor, line: Int, column: Int): Int
  fun logicalLineToVisualLine(editor: VimEditor, line: Int): Int
  fun normalizeVisualLine(editor: VimEditor, line: Int): Int
  fun normalizeVisualColumn(editor: VimEditor, line: Int, col: Int, allowEnd: Boolean): Int
  fun amountOfInlaysBeforeVisualPosition(editor: VimEditor, pos: VimVisualPosition): Int
  fun getVisualLineCount(editor: VimEditor): Int
  fun prepareLastColumn(caret: VimCaret): Int
  fun updateLastColumn(caret: VimCaret, prevLastColumn: Int)
  fun getLineEndOffset(editor: VimEditor, line: Int, allowEnd: Boolean): Int
  fun getLineStartOffset(editor: VimEditor, line: Int): Int
  fun getLineEndForOffset(editor: VimEditor, offset: Int): Int
}