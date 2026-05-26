/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import com.maddyhome.idea.vim.state.mode.inCommandLineModeWithVisual
import com.maddyhome.idea.vim.state.mode.inSelectMode
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

fun setVisualSelection(selectionStart: Int, selectionEnd: Int, caret: VimCaret) {
  val (start, end) = if (selectionStart > selectionEnd) selectionEnd to selectionStart else selectionStart to selectionEnd
  val editor = caret.editor
  val selectionType = editor.mode.selectionType ?: SelectionType.CHARACTER_WISE
  val mode = editor.mode
  when (selectionType) {
    SelectionType.CHARACTER_WISE -> {
      val (nativeStart, nativeEnd) = charToNativeSelection(editor, start, end, mode)
      caret.vimSetSystemSelectionSilently(nativeStart, nativeEnd)
    }

    SelectionType.LINE_WISE -> {
      val (nativeStart, nativeEnd) = lineToNativeSelection(editor, start, end)
      caret.vimSetSystemSelectionSilently(nativeStart, nativeEnd)
    }

    SelectionType.BLOCK_WISE -> setVirtualBlockSelection(editor, selectionStart, selectionEnd, mode)
  }

  // Selection-change notification for the IDE clipboard layer (vim-engine doesn't know how
  // PRIMARY should behave on each platform). Clipboard I/O must never break visual mode setup.
  runCatching { injector.clipboardManager.onVisualSelectionChange(caret.editor, caret) }
}

/**
 * Virtual-block path: a single primary caret plus an IDE-level highlighter band, instead of
 * N native carets. Operators read block bounds from primary's `vimSelectionStart` + `offset`
 * (see VisualOperatorActionHandler.collectSelections). Held `j`/`k` becomes O(1) per motion
 * instead of O(rows).
 */
private fun setVirtualBlockSelection(editor: VimEditor, selectionStart: Int, selectionEnd: Int, mode: Mode) {
  val (blockStart, blockEnd) = blockToNativeSelection(editor, selectionStart, selectionEnd, mode)
  val dollarExtension = editor.primaryCaret().vimLastColumn >= VimMotionGroupBase.LAST_COLUMN

  editor.removeSecondaryCarets()
  editor.primaryCaret().moveToInlayAwareOffset(selectionEnd)
  setPrimaryRowSelection(editor, blockStart, blockEnd)

  injector.blockSelectionRenderer.update(editor, blockStart, blockEnd, dollarExtension)
}

/**
 * Maintain a real IntelliJ selection on the primary caret matching the block's column range
 * *on the primary's row*. Code that reads `caret.selectionStart/End` (e.g.
 * `adjustCaretsForSelectionPolicy` during `<C-G>` Visual<->Select toggle) needs this. We
 * deliberately don't span the block diagonally — that would render in the IDE as a continuous
 * character-mode selection across rows, obscuring the per-row band painted by RangeHighlighters.
 */
private fun setPrimaryRowSelection(editor: VimEditor, blockStart: BufferPosition, blockEnd: BufferPosition) {
  val minCol = minOf(blockStart.column, blockEnd.column)
  val maxCol = maxOf(blockStart.column, blockEnd.column)
  val primaryRow = editor.primaryCaret().getBufferPosition().line
  val rowStart = editor.getLineStartOffset(primaryRow)
  val rowEnd = editor.getLineEndOffset(primaryRow)
  val selStart = (rowStart + minCol).coerceAtMost(rowEnd)
  val selEnd = (rowStart + maxCol).coerceAtMost(rowEnd)
  editor.primaryCaret().vimSetSystemSelectionSilently(selStart, selEnd)
}

/**
 * Set selection for caret
 * This method doesn't change CommandState and operates only with caret and it's properties
 * if [moveCaretToSelectionEnd] is true, caret movement to [end] will be performed
 */
fun VimCaret.vimSetSelection(start: Int, end: Int = start, moveCaretToSelectionEnd: Boolean = false) {
  vimSelectionStart = start
  val caret = if (moveCaretToSelectionEnd && !editor.inBlockSelection) moveToInlayAwareOffset(end) else this
  setVisualSelection(start, end, caret)
}

/**
 * Move selection end to current primary caret position
 *
 * This method is created only for block mode. Note that this method will invalidate all carets!
 *
 * @see vimMoveSelectionToCaret for character and line selection
 */
fun vimMoveBlockSelectionToOffset(editor: VimEditor, offset: Int) {
  val primaryCaret = editor.primaryCaret()
  val startOffsetMark = primaryCaret.vimSelectionStart

  setVisualSelection(startOffsetMark, offset, primaryCaret)
}

/**
 * Move selection end to current caret position
 * This method is created only for Character and Line mode
 * @see vimMoveBlockSelectionToOffset for blockwise selection
 */
fun VimCaret.vimMoveSelectionToCaret(vimSelectionStart: Int = this.vimSelectionStart) {
  if (!editor.inVisualMode && !editor.inSelectMode && !editor.inCommandLineModeWithVisual) error("Attempt to extent selection in non-visual mode")
  if (editor.inBlockSelection) error("Move caret with [vimMoveBlockSelectionToOffset]")

  val startOffsetMark = vimSelectionStart

  setVisualSelection(startOffsetMark, offset, this)
}

/**
 * Update selection according to new CommandState
 * This method should be used for switching from character to line wise selection and so on
 */
fun VimCaret.vimUpdateEditorSelection() {
  val startOffsetMark = vimSelectionStart
  setVisualSelection(startOffsetMark, offset, this)
}

/**
 * This works almost like [Caret.getLeadSelectionOffset] in IJ, but vim-specific
 */
@VimLockLabel.RequiresReadLock
val ImmutableVimCaret.vimLeadSelectionOffset: Int
  get() {
    val caretOffset = offset
    if (hasSelection()) {
      val selectionAdj = injector.visualMotionGroup.selectionAdj
      if (caretOffset != selectionStart && caretOffset != selectionEnd) {
        // Try to check if current selection is tweaked by fold region.
        val foldRegion = editor.getCollapsedFoldRegionAtOffset(caretOffset)
        if (foldRegion != null) {
          if (foldRegion.startOffset == selectionStart) {
            return (selectionEnd - selectionAdj).coerceAtLeast(0)
          } else if (foldRegion.endOffset == selectionEnd) {
            return selectionStart
          }
        }
      }

      return if (editor.mode.selectionType == SelectionType.LINE_WISE) {
        val selectionStartLine = editor.offsetToBufferPosition(selectionStart).line
        val caretLine = editor.offsetToBufferPosition(this.offset).line
        if (caretLine == selectionStartLine) {
          val column = editor.offsetToBufferPosition(selectionEnd).column
          if (column == 0) (selectionEnd - 1).coerceAtLeast(0) else selectionEnd
        } else {
          selectionStart
        }
      } else if (editor.inBlockSelection) {
        val selections = editor.nativeCarets().map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
        val pCaret = editor.primaryCaret()
        when (pCaret.offset) {
          selections.first().first -> (selections.last().second - selectionAdj).coerceAtLeast(0)
          selections.first().second -> selections.last().first
          selections.last().first -> (selections.first().second - selectionAdj).coerceAtLeast(0)
          selections.last().second -> selections.first().first
          else -> selections.first().first
        }
      } else {
        if (caretOffset == selectionStart) (selectionEnd - selectionAdj).coerceAtLeast(0) else selectionStart
      }
    }
    return caretOffset
  }
