/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.RWLockLabel
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import kotlin.math.max
import kotlin.math.min

/**
 * Represents information about a text selection, primarily utilized for marking selection boundaries.
 * This class is used to track the start and end points of a selection. The values of [start] and [end] can be null,
 * indicating situations where either the start or end of a selection mark was removed, or when a single mark was set manually without a real selection.
 * It is important to note that [start] and [end] are not necessarily in sequential order. The [start] represents the position where the caret begins, and [end] where it stops.
 * This allows for the caret to move in reverse order, thus [start] and [end] are not guaranteed to be in any specific order.
 * The [start] property is not the actual start offset of the selection but represents the '<' mark. In the case of line-wise selection, it will be the offset at the start of the line.
 * Similarly, the [end] property is not the actual end offset of the selection but represents the '>' mark. For line-wise selections, this will be the offset at the end of the line.
 *
 * @property start          The BufferPosition marking the start of the selection or caret.
 * @property end            The BufferPosition marking the end of the selection or caret.
 * @property selectionType  The type of selection being represented (character-wise, line-wise, etc.).
 */
data class SelectionInfo(var start: BufferPosition?, var end: BufferPosition?, val selectionType: SelectionType) {
  /**
   * Provides the start and end BufferPositions in sorted order as a Pair. This property ensures a sequential
   * order of positions, regardless of the caret movement direction.
   */
  val startEndSorted: Pair<BufferPosition, BufferPosition>? get() = sortBufferPositions(start, end)

  fun getSelectionRange(editor: VimEditor): TextRange? {
    val (sortedStart, sortedEnd) = startEndSorted ?: return null
    return when (selectionType) {
      SelectionType.CHARACTER_WISE -> TextRange(
        editor.bufferPositionToOffset(sortedStart),
        editor.bufferPositionToOffset(sortedEnd) + 1
      )

      SelectionType.LINE_WISE -> {
        val startOffset = editor.getLineStartOffset(sortedStart.line)
        val endOffset = editor.getLineEndOffset(sortedEnd.line, true) + 1
        return TextRange(startOffset, endOffset)
      }

      SelectionType.BLOCK_WISE -> {
        val topLine = sortedStart.line
        val bottomLine = sortedEnd.line
        val leftColumn = min(sortedStart.column, sortedEnd.column)
        val rightColumn = max(sortedStart.column, sortedEnd.column)

        val startOffsets = (topLine..bottomLine).map { editor.getOffset(it, leftColumn) }.toIntArray()
        val endOffsets = (topLine..bottomLine).map { editor.getOffset(it, rightColumn) + 1 }.toIntArray()
        return TextRange(startOffsets, endOffsets)
      }
    }
  }

  fun isSelected(offset: Int, editor: VimEditor): Boolean {
    return getSelectionRange(editor)?.contains(offset) ?: false
  }

  private fun sortBufferPositions(pos1: BufferPosition?, pos2: BufferPosition?): Pair<BufferPosition, BufferPosition>? {
    if (pos1 == null || pos2 == null) return null
    return if (pos1.line != pos2.line) {
      if (pos1.line < pos2.line) Pair(pos1, pos2) else Pair(pos2, pos1)
    } else {
      if (pos1.column < pos2.column) Pair(pos1, pos2) else Pair(pos2, pos1)
    }
  }

  companion object {
    @RWLockLabel.Readonly
    fun collectCurrentSelectionInfo(caret: VimCaret): SelectionInfo? {
      val editor = caret.editor
      val mode = editor.mode

      if (mode !is Mode.VISUAL) return null
      val start = editor.offsetToBufferPosition(caret.vimSelectionStart)
      val end = editor.offsetToBufferPosition(caret.offset)
      return SelectionInfo(start, end, mode.selectionType)
    }
  }
}
