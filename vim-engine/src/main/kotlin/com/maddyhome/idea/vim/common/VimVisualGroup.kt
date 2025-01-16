/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.state.mode.Mode

fun charToNativeSelection(editor: VimEditor, start: Int, end: Int, mode: Mode): Pair<Int, Int> {
  val (nativeStart, nativeEnd) = sort(start, end)
  // TODO: Remove this unnecessary restriction on not selecting the new line character
  // When doing so, please remove the hack in VimSearchHelperBase.findWordUnderCursor
  val lineEnd = editor.getLineEndForOffset(nativeEnd)
  val adj = if (isExclusiveSelection() || nativeEnd == lineEnd || mode is Mode.SELECT) 0 else 1
  val adjEnd = (nativeEnd + adj).coerceAtMost(editor.fileSize().toInt())
  return nativeStart to adjEnd
}

/**
 * Convert vim's selection start and end to corresponding native selection.
 *
 * Adds caret adjustment or extends to line start / end in case of linewise selection
 */
fun lineToNativeSelection(editor: VimEditor, start: Int, end: Int): Pair<Int, Int> {
  val (nativeStart, nativeEnd) = sort(start, end)
  val lineStart = editor.getLineStartForOffset(nativeStart)
  // Extend to \n char of line to fill full line with selection
  val lineEnd = (editor.getLineEndForOffset(nativeEnd) + 1).coerceAtMost(editor.fileSize().toInt())
  return lineStart to lineEnd
}

fun <T : Comparable<T>> sort(a: T, b: T): Pair<T, T> = if (a > b) b to a else a to b

private fun isExclusiveSelection() = injector.globalOptions().selection.contains("exclusive")

fun blockToNativeSelection(
  editor: VimEditor,
  start: Int,
  end: Int,
  mode: Mode,
): Pair<BufferPosition, BufferPosition> {
  var blockStart = editor.offsetToBufferPosition(start)
  var blockEnd = editor.offsetToBufferPosition(end)
  if (!isExclusiveSelection() && mode !is Mode.SELECT) {
    if (blockStart.column > blockEnd.column) {
      if (blockStart.column < editor.lineLength(blockStart.line)) {
        blockStart = BufferPosition(blockStart.line, blockStart.column + 1)
      }
    } else {
      if (blockEnd.column < editor.lineLength(blockEnd.line)) {
        blockEnd = BufferPosition(blockEnd.line, blockEnd.column + 1)
      }
    }
  }
  return blockStart to blockEnd
}
