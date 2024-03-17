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
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE
import com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE
import com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE
import com.maddyhome.idea.vim.common.Pointer
import com.maddyhome.idea.vim.common.TextRange
import org.jetbrains.annotations.NonNls
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
public sealed class VimSelection {
  public abstract val type: SelectionType
  public abstract val vimStart: Int
  public abstract val vimEnd: Int
  protected abstract val editor: VimEditor

  public abstract fun toVimTextRange(skipNewLineForLineMode: Boolean = false): TextRange

  public abstract fun getNativeStartAndEnd(): Pair<Int, Int>

  public companion object {
    public fun create(vimStart: Int, vimEnd: Int, type: SelectionType, editor: VimEditor): VimSelection = when (type) {
      CHARACTER_WISE -> {
        val nativeSelection = charToNativeSelection(editor, vimStart, vimEnd, Mode.VISUAL(SelectionType.CHARACTER_WISE))
        VimCharacterSelection(vimStart, vimEnd, nativeSelection.first, nativeSelection.second, editor)
      }
      LINE_WISE -> {
        val nativeSelection = lineToNativeSelection(editor, vimStart, vimEnd)
        VimLineSelection(vimStart, vimEnd, nativeSelection.first, nativeSelection.second, editor)
      }
      BLOCK_WISE -> VimBlockSelection(vimStart, vimEnd, editor, false)
    }
  }

  @NonNls
  override fun toString(): String {
    val startLogPosition = editor.offsetToBufferPosition(vimStart)
    val endLogPosition = editor.offsetToBufferPosition(vimEnd)
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
public sealed class VimSimpleSelection : VimSelection() {
  public abstract val nativeStart: Int
  public abstract val nativeEnd: Int
  public abstract val normNativeStart: Int
  public abstract val normNativeEnd: Int

  override fun getNativeStartAndEnd(): Pair<Int, Int> = normNativeStart to normNativeEnd

  public companion object {
    /**
     * Create character- and linewise selection if native selection is already known. Doesn't work for block selection
     */
    public fun createWithNative(
      vimStart: Int,
      vimEnd: Int,
      nativeStart: Int,
      nativeEnd: Int,
      type: SelectionType,
      editor: VimEditor,
    ): VimSimpleSelection = when (type) {
      CHARACTER_WISE -> VimCharacterSelection(vimStart, vimEnd, nativeStart, nativeEnd, editor)
      LINE_WISE -> VimLineSelection(vimStart, vimEnd, nativeStart, nativeEnd, editor)
      BLOCK_WISE -> error("This method works only for line and character selection")
    }
  }
}

public class VimCharacterSelection(
  override val vimStart: Int,
  override val vimEnd: Int,
  override val nativeStart: Int,
  override val nativeEnd: Int,
  override val editor: VimEditor,
) : VimSimpleSelection() {
  override val normNativeStart: Int = min(nativeStart, nativeEnd)
  override val normNativeEnd: Int = max(nativeStart, nativeEnd)
  override val type: SelectionType = CHARACTER_WISE

  override fun toVimTextRange(skipNewLineForLineMode: Boolean): TextRange = TextRange(normNativeStart, normNativeEnd)
}

public class VimLineSelection(
  override val vimStart: Int,
  override val vimEnd: Int,
  override val nativeStart: Int,
  override val nativeEnd: Int,
  override val editor: VimEditor,
) : VimSimpleSelection() {
  override val normNativeStart: Int = min(nativeStart, nativeEnd)
  override val normNativeEnd: Int = max(nativeStart, nativeEnd)
  override val type: SelectionType = LINE_WISE

  override fun toVimTextRange(skipNewLineForLineMode: Boolean): TextRange =
    if (skipNewLineForLineMode && editor.fileSize() >= normNativeEnd && normNativeEnd > 0 && editor.charAt(Pointer(normNativeEnd - 1)) == '\n') {
      TextRange(normNativeStart, (normNativeEnd - 1).coerceAtLeast(0))
    } else {
      TextRange(normNativeStart, normNativeEnd)
    }
}

public class VimBlockSelection(
  override val vimStart: Int,
  override val vimEnd: Int,
  override val editor: VimEditor,
  private val toLineEnd: Boolean,
) : VimSelection() {
  override fun getNativeStartAndEnd(): Pair<Int, Int> = blockToNativeSelection(editor, vimStart, vimEnd, Mode.VISUAL(
    SelectionType.CHARACTER_WISE)).let {
    editor.bufferPositionToOffset(it.first) to editor.bufferPositionToOffset(it.second)
  }

  override val type: SelectionType = BLOCK_WISE

  override fun toVimTextRange(skipNewLineForLineMode: Boolean): TextRange {
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()
    forEachLine { start, end ->
      starts += start
      ends += end
    }
    return TextRange(starts.toIntArray(), ends.toIntArray()).also { it.normalize(editor.fileSize().toInt()) }
  }

  private fun forEachLine(action: (start: Int, end: Int) -> Unit) {
    val (startPosition, endPosition) = blockToNativeSelection(editor, vimStart, vimEnd, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    val lineRange =
      if (startPosition.line > endPosition.line) endPosition.line..startPosition.line else startPosition.line..endPosition.line
    lineRange.map { line ->
      val start = editor.bufferPositionToOffset(BufferPosition(line, startPosition.column))
      val end = if (toLineEnd) {
        editor.getLineEndOffset(line, true)
      } else {
        editor.bufferPositionToOffset(BufferPosition(line, endPosition.column))
      }
      action(start, end)
    }
  }
}
