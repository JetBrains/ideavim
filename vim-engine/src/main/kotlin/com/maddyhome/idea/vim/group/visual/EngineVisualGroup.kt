/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.isLineEmpty
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.listener.CaretVisualAttributesListenerSuppressor
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

    SelectionType.BLOCK_WISE -> {
      // Suppress our own EditorCaretHandler during the bulk caret replacement — without this,
      // each of the N caretAdded/caretRemoved events fired by setBlockSelection triggers an
      // O(N) `updateCaretsVisualAttributes` walk, giving O(N²) per block-visual motion and an
      // unresponsive EDT on large blocks. The single update at the end of this block is enough.
      //
      // runAsAllCaretsAction fires the all-carets-action listener pair so split mode's
      // PatchEngineEditorSynchronizer batches its per-event protocol sends into one snapshot.
      // Safe here because the body doesn't run any IDE actions (only Vim-internal caret moves).
      CaretVisualAttributesListenerSuppressor.lock().use {
        editor.runAsAllCaretsAction {
          // This will invalidate any secondary carets, but we shouldn't have any of these cached
          // in local variables, etc. Required for correctness: setCaretsAndSelections reuses
          // existing carets by insertion order, and Vim's `vimSelectionStart` user-data is
          // attached to a specific caret instance — if a different caret becomes primary
          // (e.g. after `O`-swap then `k` shrinking the block), the anchor stored on the now-
          // removed caret is lost. Pre-clearing ensures primary is always the same caret
          // throughout a block-visual session.
          editor.removeSecondaryCarets()

          // WARNING! This can invalidate the primary caret! I.e. the `caret` parameter will no
          // longer be the primary caret. Given an existing visual block selection, moving the
          // caret will first remove all secondary carets (above) then this method will ask
          // IntelliJ to create a new multi-caret block selection. If we're moving up (`k`) a new
          // caret is added, and becomes the new primary caret. The current `caret` parameter
          // remains valid, but is no longer the primary caret. Make sure to fetch the new
          // primary caret if necessary.
          val (blockStart, blockEnd) = blockToNativeSelection(editor, selectionStart, selectionEnd, mode)
          val lastColumn = editor.primaryCaret().vimLastColumn
          editor.vimSetSystemBlockSelectionSilently(blockStart, blockEnd)

          for (aCaret in editor.nativeCarets()) {
            if (!aCaret.isValid) continue
            val line = aCaret.getBufferPosition().line
            val lineEndOffset = editor.getLineEndOffset(line, true)
            val lineStartOffset = editor.getLineStartOffset(line)

            // Extend selection to line end if it was made with `$` command
            if (lastColumn >= VimMotionGroupBase.LAST_COLUMN) {
              aCaret.vimSetSystemSelectionSilently(aCaret.selectionStart, lineEndOffset)
              val newOffset = (lineEndOffset - injector.visualMotionGroup.selectionAdj).coerceAtLeast(lineStartOffset)
              aCaret.moveToInlayAwareOffset(newOffset)
            }
            val visualPosition = editor.offsetToVisualPosition(aCaret.selectionEnd)
            if (aCaret.offset == aCaret.selectionEnd && visualPosition != aCaret.getVisualPosition()) {
              // Put right caret position for tab character
              aCaret.moveToVisualPosition(visualPosition)
            }
            if (mode !is Mode.SELECT &&
              !editor.isLineEmpty(line, false) &&
              aCaret.offset == aCaret.selectionEnd &&
              aCaret.selectionEnd - 1 >= lineStartOffset &&
              aCaret.selectionEnd - aCaret.selectionStart != 0
            ) {
              // Move all carets one char left in case if it's on selection end
              aCaret.moveToVisualPosition(VimVisualPosition(visualPosition.line, visualPosition.column - 1))
            }
          }

          editor.primaryCaret().moveToInlayAwareOffset(selectionEnd)
        }
      }

      // Hide secondary carets to better emulate block selection (suppressor skipped this
      // during the bulk update).
      injector.editorGroup.updateCaretsVisualAttributes(editor)
    }
  }

  // Selection-change notification for the IDE clipboard layer (vim-engine doesn't know how
  // PRIMARY should behave on each platform). Clipboard I/O must never break visual mode setup.
  runCatching { injector.clipboardManager.onVisualSelectionChange(caret.editor, caret) }
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
