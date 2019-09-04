/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.SelectionType.*
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.EditorHelper
import kotlin.math.max
import kotlin.math.min

/**
 * @author Alex Plate
 *
 * Interface for storing selection range.
 *
 * Type of selection is stored in [type]
 * [vimStart] and [vimEnd] - selection offsets in vim model. There values will be stored in '< and '> marks.
 *   Actually [vimStart] - initial caret position when visual mode entered and [vimEnd] - current caret position.
 *
 * This selection has direction. That means that by moving in left-up direction (e.g. `vbbbb`)
 *   [vimStart] will be greater then [vimEnd].
 *
 * All starts are included and ends are excluded
 */
sealed class VimSelection {
  abstract val type: SelectionType
  abstract val vimStart: Int
  abstract val vimEnd: Int
  protected abstract val editor: Editor

  abstract fun toVimTextRange(skipNewLineForLineMode: Boolean = false): TextRange

  abstract fun getNativeStartAndEnd(): Pair<Int, Int>

  companion object {
    fun create(vimStart: Int, vimEnd: Int, type: SelectionType, editor: Editor) = when (type) {
      CHARACTER_WISE -> {
        val nativeSelection = charToNativeSelection(editor, vimStart, vimEnd, CommandState.Mode.VISUAL)
        VimCharacterSelection(vimStart, vimEnd, nativeSelection.first, nativeSelection.second, editor)
      }
      LINE_WISE -> {
        val nativeSelection = lineToNativeSelection(editor, vimStart, vimEnd)
        VimLineSelection(vimStart, vimEnd, nativeSelection.first, nativeSelection.second, editor)
      }
      BLOCK_WISE -> VimBlockSelection(vimStart, vimEnd, editor, false)
    }
  }

  override fun toString(): String {
    val startLogPosition = editor.offsetToLogicalPosition(vimStart)
    val endLogPosition = editor.offsetToLogicalPosition(vimEnd)
    return "Selection [$type]: vim start[offset: $vimStart : col ${startLogPosition.column} line ${startLogPosition.line}]" +
      " vim end[offset: $vimEnd : col ${endLogPosition.column} line ${endLogPosition.line}]"
  }
}

/**
 * Interface for storing simple selection range.
 *   Simple means that this selection can be represented only by start and end values.
 *   There selections in vim are character- and linewise selections.
 *
 *  [nativeStart] and [nativeEnd] are the offsets of native selection
 *
 * [vimStart] and [vimEnd] - selection offsets in vim model. There values will be stored in '< and '> marks.
 *   There values can differ from [nativeStart] and [nativeEnd] in case of linewise selection because [vimStart] - initial caret
 *   position when visual mode entered and [vimEnd] - current caret position.
 *
 * This selection has direction. That means that by moving in left-up direction (e.g. `vbbbb`)
 *   [nativeStart] will be greater than [nativeEnd].
 * If you need normalized [nativeStart] and [nativeEnd] (start always less than end) you
 *   can use [normNativeStart] and [normNativeEnd]
 *
 * All starts are included and ends are excluded
 */
sealed class VimSimpleSelection : VimSelection() {
  abstract val nativeStart: Int
  abstract val nativeEnd: Int
  abstract val normNativeStart: Int
  abstract val normNativeEnd: Int

  override fun getNativeStartAndEnd() = normNativeStart to normNativeEnd

  companion object {
    /**
     * Create character- and linewise selection if native selection is already known. Doesn't work for block selection
     */
    fun createWithNative(vimStart: Int, vimEnd: Int, nativeStart: Int, nativeEnd: Int, type: SelectionType, editor: Editor) =
      when (type) {
        CHARACTER_WISE -> VimCharacterSelection(vimStart, vimEnd, nativeStart, nativeEnd, editor)
        LINE_WISE -> VimLineSelection(vimStart, vimEnd, nativeStart, nativeEnd, editor)
        BLOCK_WISE -> throw RuntimeException("This method works only for line and character selection")
      }
  }
}

class VimCharacterSelection(
  override val vimStart: Int,
  override val vimEnd: Int,
  override val nativeStart: Int,
  override val nativeEnd: Int,
  override val editor: Editor
) : VimSimpleSelection() {
  override val normNativeStart = min(nativeStart, nativeEnd)
  override val normNativeEnd = max(nativeStart, nativeEnd)
  override val type: SelectionType = CHARACTER_WISE

  override fun toVimTextRange(skipNewLineForLineMode: Boolean) = TextRange(normNativeStart, normNativeEnd)
}

class VimLineSelection(
  override val vimStart: Int,
  override val vimEnd: Int,
  override val nativeStart: Int,
  override val nativeEnd: Int,
  override val editor: Editor
) : VimSimpleSelection() {
  override val normNativeStart = min(nativeStart, nativeEnd)
  override val normNativeEnd = max(nativeStart, nativeEnd)
  override val type = LINE_WISE

  override fun toVimTextRange(skipNewLineForLineMode: Boolean) =
    if (skipNewLineForLineMode && editor.document.textLength >= normNativeEnd && normNativeEnd > 0 && editor.document.text[normNativeEnd - 1] == '\n') {
      TextRange(normNativeStart, (normNativeEnd - 1).coerceAtLeast(0))
    } else {
      TextRange(normNativeStart, normNativeEnd)
    }
}

class VimBlockSelection(
  override val vimStart: Int,
  override val vimEnd: Int,
  override val editor: Editor,
  val toLineEnd: Boolean
) : VimSelection() {
  override fun getNativeStartAndEnd() = blockToNativeSelection(editor, vimStart, vimEnd, CommandState.Mode.VISUAL).let {
    editor.logicalPositionToOffset(it.first) to editor.logicalPositionToOffset(it.second)
  }

  override val type = BLOCK_WISE

  override fun toVimTextRange(skipNewLineForLineMode: Boolean): TextRange {
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()
    forEachLine { start, end ->
      starts += start
      ends += end
    }
    return TextRange(starts.toIntArray(), ends.toIntArray()).also { it.normalize(editor.document.textLength) }
  }

  private fun forEachLine(action: (start: Int, end: Int) -> Unit) {
    val (logicalStart, logicalEnd) = blockToNativeSelection(editor, vimStart, vimEnd, CommandState.Mode.VISUAL)
    val lineRange = if (logicalStart.line > logicalEnd.line) logicalEnd.line..logicalStart.line else logicalStart.line..logicalEnd.line
    lineRange.map { line ->
      val start = editor.logicalPositionToOffset(LogicalPosition(line, logicalStart.column))
      val end = if (toLineEnd) {
        EditorHelper.getLineEndOffset(editor, line, true)
      } else {
        editor.logicalPositionToOffset(LogicalPosition(line, logicalEnd.column))
      }
      action(start, end)
    }
  }
}
