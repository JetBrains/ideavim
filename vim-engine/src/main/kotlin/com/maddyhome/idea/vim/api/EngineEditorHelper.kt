/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import java.nio.CharBuffer

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
  fun getLineStartForOffset(editor: VimEditor, line: Int): Int
  fun getLineEndForOffset(editor: VimEditor, offset: Int): Int
  fun visualLineToLogicalLine(editor: VimEditor, line: Int): Int
  fun normalizeLine(editor: VimEditor, line: Int): Int
  fun getVisualLineAtTopOfScreen(editor: VimEditor): Int
  fun getApproximateScreenWidth(editor: VimEditor): Int
  fun handleWithReadonlyFragmentModificationHandler(editor: VimEditor, exception: java.lang.Exception)
  fun getLineBuffer(editor: VimEditor, line: Int): CharBuffer
  fun getVisualLineAtBottomOfScreen(editor: VimEditor): Int
  fun pad(editor: VimEditor, context: ExecutionContext, line: Int, to: Int): String
  fun getLineLength(editor: VimEditor, logicalLine: Int): Int
  fun getLineLength(editor: VimEditor): Int
  fun getLineBreakCount(text: CharSequence): Int
  fun inlayAwareOffsetToVisualPosition(editor: VimEditor, offset: Int): VimVisualPosition
  fun getVisualLineLength(editor: VimEditor, line: Int): Int
  fun getLeadingWhitespace(editor: VimEditor, line: Int): String
  fun anyNonWhitespace(editor: VimEditor, offset: Int, dir: Int): Boolean
}

fun VimEditor.endsWithNewLine(): Boolean {
  val textLength = this.fileSize().toInt()
  if (textLength == 0) return false
  return this.text()[textLength - 1] == '\n'
}
