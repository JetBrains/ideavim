/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimBlockSelectionRenderer
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.helper.resetVimLastColumn
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inBlockSelection

/**
 * Renders block-visual selection using IDE-level [RangeHighlighter]s plus a single primary
 * caret, instead of N native IntelliJ carets. Operators read block bounds from the primary
 * caret's `vimSelectionStart` + `offset` (see `VisualOperatorActionHandler.collectSelections`)
 * so N native carets are not required for correctness.
 *
 * Gated behind the `ideavim.virtual.block` system property. Default: off.
 */
internal class IjBlockSelectionRenderer : VimBlockSelectionRenderer, ModeChangeListener {
  private val highlightersPerEditor = HashMap<Editor, List<RangeHighlighter>>()

  // Primary's IntelliJ selection alone can't serve as the bounds source — it clamps at line
  // ends, losing the block's logical width on short rows.
  private val blockBoundsPerEditor = HashMap<Editor, VimBlockSelectionRenderer.BlockBounds>()

  // Snapshot of bounds at the most-recent block-visual exit, consumed by ex commands and the
  // undo fallback that run in Normal mode after the exit (see commandsSinceExitPerEditor).
  private val lastExitedBoundsPerEditor = HashMap<Editor, VimBlockSelectionRenderer.BlockBounds>()

  // Non-undo commands seen since the most recent block-visual exit. Lets the undo fallback
  // tell "undo of the immediate destructive block edit" (≤ 1) from "undo of a later op that
  // happened to follow a block-visual exit" (> 1), e.g. `<C-V>jjl y $ p u`.
  private val commandsSinceExitPerEditor = HashMap<Editor, Int>()

  init {
    ensureModeChangeListenerRegistered()
  }

  override fun isEnabled(editor: VimEditor): Boolean {
    return editor is IjVimEditor
  }

  override fun notifyCommandFinished(editor: VimEditor) {
    val ij = (editor as? IjVimEditor)?.editor ?: return
    tickCommandsSinceExit(ij)
    if (editor.inBlockSelection) dropIntendedColumnIfPrimaryOnTab(ij)
  }

  fun consumeLastExitedBounds(editor: VimEditor): VimBlockSelectionRenderer.BlockBounds? {
    val ij = (editor as? IjVimEditor)?.editor ?: return null
    return lastExitedBoundsPerEditor.remove(ij)
  }

  fun commandsSinceExit(editor: VimEditor): Int? {
    val ij = (editor as? IjVimEditor)?.editor ?: return null
    return commandsSinceExitPerEditor[ij]
  }

  private fun ensureModeChangeListenerRegistered() {
    val listeners = injector.listenersNotifier.modeChangeListeners
    if (this !in listeners) listeners.add(this)
  }

  private fun tickCommandsSinceExit(ij: Editor) {
    commandsSinceExitPerEditor[ij]?.let { commandsSinceExitPerEditor[ij] = it + 1 }
  }

  /**
   * Native block-visual rebuilds carets each motion, so the action handler's
   * `caret.vimLastColumn = motion.intendedColumn` becomes a no-op on the new primary and the
   * next motion tracks the achieved visual column — exactly what tab per-row clamping needs.
   * Virtual-block keeps the same caret, so when primary lands on a tab we drop the intended
   * column here to restore the native behaviour. Other "adjusted offset" cases (short/empty
   * lines) want the intent preserved so the block re-widens on later longer rows.
   */
  private fun dropIntendedColumnIfPrimaryOnTab(ij: Editor) {
    val primary = ij.caretModel.primaryCaret
    if (primary.vimLastColumn >= VimMotionGroupBase.LAST_COLUMN) return
    val text = ij.document.charsSequence
    if (primary.offset < text.length && text[primary.offset] == '\t') {
      primary.resetVimLastColumn()
    }
  }

  override fun update(
    editor: VimEditor,
    blockStart: BufferPosition,
    blockEnd: BufferPosition,
    dollarExtension: Boolean,
  ) {
    ensureModeChangeListenerRegistered()
    val ij = (editor as? IjVimEditor)?.editor ?: return
    blockBoundsPerEditor[ij] = VimBlockSelectionRenderer.BlockBounds(
      blockStart.line, blockStart.column, blockEnd.line, blockEnd.column, dollarExtension,
    )
    repaintHighlighterBand(ij, blockStart, blockEnd, dollarExtension)
  }

  private fun repaintHighlighterBand(
    ij: Editor,
    blockStart: BufferPosition,
    blockEnd: BufferPosition,
    dollarExtension: Boolean,
  ) {
    clearHighlightersOnly(ij)
    val minLine = minOf(blockStart.line, blockEnd.line)
    val maxLine = maxOf(blockStart.line, blockEnd.line)
    val minCol = minOf(blockStart.column, blockEnd.column)
    val maxCol = maxOf(blockStart.column, blockEnd.column)
    val attributes = selectionAttributes(ij)
    val highlighters = ArrayList<RangeHighlighter>(maxLine - minLine + 1)
    for (line in minLine..maxLine) {
      addRowHighlighter(ij, line, minCol, maxCol, dollarExtension, attributes)?.let(highlighters::add)
    }
    highlightersPerEditor[ij] = highlighters
  }

  private fun addRowHighlighter(
    ij: Editor,
    line: Int,
    minCol: Int,
    maxCol: Int,
    dollarExtension: Boolean,
    attributes: TextAttributes,
  ): RangeHighlighter? {
    val startOffset = ij.logicalPositionToOffset(LogicalPosition(line, minCol))
    val endOffset = if (dollarExtension) {
      ij.document.getLineEndOffset(line)
    } else {
      ij.logicalPositionToOffset(LogicalPosition(line, maxCol))
    }
    if (endOffset <= startOffset) return null
    return ij.markupModel.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.SELECTION - 1,
      attributes,
      HighlighterTargetArea.EXACT_RANGE,
    )
  }

  override fun clear(editor: VimEditor) {
    val ij = (editor as? IjVimEditor)?.editor ?: return
    clearFor(ij)
  }

  override fun getBounds(editor: VimEditor): VimBlockSelectionRenderer.BlockBounds? {
    val ij = (editor as? IjVimEditor)?.editor ?: return null
    return blockBoundsPerEditor[ij]
  }

  override fun materializeCarets(editor: VimEditor) {
    materializeFromBounds(editor) { blockBoundsPerEditor[it] }
  }

  override fun materializeCaretsFromSavedExit(editor: VimEditor) {
    materializeFromBounds(editor) { lastExitedBoundsPerEditor[it] }
  }

  private fun materializeFromBounds(
    editor: VimEditor,
    boundsLookup: (Editor) -> VimBlockSelectionRenderer.BlockBounds?,
  ) {
    if (editor !is IjVimEditor) return
    ApplicationManager.getApplication().invokeAndWait {
      val ij = editor.editor
      val mode = editor.mode
      val bounds = boundsLookup(ij)
      if (bounds == null) {
        defendAgainstStaleBlockState(ij, mode)
        return@invokeAndWait
      }
      applyCaretStatesPreservingVimEngineState(editor, ij, bounds, isSelectMode(mode))
      clearHighlightersOnly(ij)
    }
  }

  /**
   * The mode-change listener that normally clears block state may have been reset (e.g. test
   * teardown). If we still hold bounds/highlighters while the editor is no longer in a
   * block-wise mode, drop them ourselves. The hasStaleState guard avoids wiping legitimate
   * non-block selections (e.g. one created by `:action EditorSelectWord` in Normal mode).
   */
  private fun defendAgainstStaleBlockState(ij: Editor, mode: Mode) {
    val hasStaleState = blockBoundsPerEditor.containsKey(ij) || highlightersPerEditor.containsKey(ij)
    if (hasStaleState && !isBlockWiseMode(mode)) dropStaleBlockState(ij, mode)
  }

  /**
   * Replace the IntelliJ caret set in a single bulk call (one event dispatch instead of N) and
   * restore the bits of vim-engine state that `setCaretsAndSelections` invalidates by replacing
   * the primary IJ caret wholesale. Critical for 500+ row blocks.
   */
  private fun applyCaretStatesPreservingVimEngineState(
    editor: IjVimEditor,
    ij: Editor,
    bounds: VimBlockSelectionRenderer.BlockBounds,
    selectMode: Boolean,
  ) {
    val primaryOffsetBefore = ij.caretModel.primaryCaret.offset
    val savedVimLastColumn = editor.primaryCaret().vimLastColumn
    val savedVimSelectionStart = editor.primaryCaret().vimSelectionStart

    val states = buildCaretStates(ij, bounds, selectMode)
    ij.caretModel.setCaretsAndSelections(states)

    // The last CaretState's caretPosition becomes primary, but for $-extended rows we placed
    // it at rowEnd-1, not at the user's pre-materialize offset — restore that.
    if (ij.caretModel.primaryCaret.offset != primaryOffsetBefore) {
      ij.caretModel.primaryCaret.moveToOffset(primaryOffsetBefore)
    }
    // The setter pins _vimLastColumnPos to the new visualPosition so the getter's invalidation
    // check (UserDataManager.Caret.vimLastColumn) won't reset it on the next read.
    val newPrimary = editor.primaryCaret()
    newPrimary.vimLastColumn = savedVimLastColumn
    newPrimary.vimSelectionStart = savedVimSelectionStart
  }

  /**
   * Uses [EditorModificationUtil.calcBlockSelectionState] as the base so column clamping and
   * empty-row filtering match IntelliJ's `setBlockSelection` exactly, then bakes per-row
   * adjustments in so the full caret set can be applied with one
   * [com.intellij.openapi.editor.CaretModel.setCaretsAndSelections] call.
   */
  private fun buildCaretStates(
    ij: Editor,
    bounds: VimBlockSelectionRenderer.BlockBounds,
    selectMode: Boolean,
  ): List<CaretState> {
    val anchor = LogicalPosition(bounds.anchorLine, bounds.anchorCol)
    val active = LogicalPosition(bounds.activeLine, bounds.activeCol)
    val baseStates = EditorModificationUtil.calcBlockSelectionState(ij, anchor, active)
    if (selectMode) return baseStates
    return baseStates.map { state ->
      val caretLine = state.caretPosition?.line ?: return@map state
      if (bounds.dollarExtension) {
        dollarExtendedCaretState(ij, caretLine, state.selectionStart)
      } else {
        inclusiveEndAdjustedCaretState(ij, state) ?: state
      }
    }
  }

  private fun dollarExtendedCaretState(
    ij: Editor,
    caretLine: Int,
    selectionStart: LogicalPosition?,
  ): CaretState {
    val rowStart = ij.document.getLineStartOffset(caretLine)
    val rowEnd = ij.document.getLineEndOffset(caretLine)
    val selEnd = ij.offsetToLogicalPosition(rowEnd)
    val caret = ij.offsetToLogicalPosition((rowEnd - 1).coerceAtLeast(rowStart))
    return CaretState(caret, selectionStart, selEnd)
  }

  /**
   * Visual char-wise: caret sits one VISUAL cell left of the exclusive selection end, so tabs
   * are walked one display cell at a time. Returns null when this row has no adjustable
   * selection (empty line / zero-width range).
   */
  private fun inclusiveEndAdjustedCaretState(ij: Editor, state: CaretState): CaretState? {
    val caretLine = state.caretPosition?.line ?: return null
    val rowStart = ij.document.getLineStartOffset(caretLine)
    val rowEnd = ij.document.getLineEndOffset(caretLine)
    val selStart = state.selectionStart ?: return null
    val selEnd = state.selectionEnd ?: return null
    val selStartOffset = ij.logicalPositionToOffset(selStart)
    val selEndOffset = ij.logicalPositionToOffset(selEnd)
    if (rowEnd <= rowStart || selEndOffset <= selStartOffset || selEndOffset - 1 < rowStart) return null

    val visualPos = ij.offsetToVisualPosition(selEndOffset)
    val adjusted = VisualPosition(visualPos.line, (visualPos.column - 1).coerceAtLeast(0))
    val adjustedLog = ij.visualToLogicalPosition(adjusted)
    // For multi-visual-cell chars (tabs, inlays), the logical position can resolve to an
    // earlier visual column than `adjusted`. visualColumnAdjustment pins the effective visual
    // position to `adjusted`, matching the old moveToVisualPosition behaviour.
    val backToVisual = ij.logicalToVisualPosition(adjustedLog)
    val visualAdjust = adjusted.column - backToVisual.column
    return CaretState(adjustedLog, visualAdjust, selStart, selEnd)
  }

  private fun dropStaleBlockState(ij: Editor, mode: Mode) {
    clearFor(ij)
    // Only clear primary's IJ selection when we're back in NORMAL — VISUAL/SELECT char- or
    // line-wise own a valid selection set elsewhere.
    if (mode is Mode.NORMAL && ij.caretModel.primaryCaret.hasSelection()) {
      ij.caretModel.primaryCaret.removeSelection()
    }
  }

  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    // Only act when leaving block-wise selection entirely. Transitions between
    // VISUAL(BLOCK_WISE), SELECT(BLOCK_WISE), and CMD_LINE wrapping either should preserve the
    // band — block selection stays semantically active even though the mode changes.
    if (!isBlockWiseMode(oldMode) || isBlockWiseMode(editor.mode)) return
    if (editor !is IjVimEditor) return

    snapshotBoundsOnExit(editor.editor)
    clear(editor)
    dropPrimaryRowSelection(editor.editor)
  }

  /**
   * Move active bounds into the dedicated "last exited" slot so Normal-mode followups can use
   * them: ex commands from `'<,'>` block-visual ranges (`Command.execute`) re-materialize
   * carets, and the undo fallback positions primary at the block's top-left.
   */
  private fun snapshotBoundsOnExit(ij: Editor) {
    blockBoundsPerEditor[ij]?.let {
      lastExitedBoundsPerEditor[ij] = it
      commandsSinceExitPerEditor[ij] = 0
    }
  }

  /**
   * Primary's IJ selection was set on its own row during the virtual block phase. Mode exit
   * (`x` delete, `<Esc>`, ...) doesn't go through any path that clears it, so do it here.
   */
  private fun dropPrimaryRowSelection(ij: Editor) {
    val primary = ij.caretModel.primaryCaret
    if (primary.hasSelection()) primary.removeSelection()
  }

  private fun isBlockWiseMode(mode: Mode): Boolean {
    val effective = if (mode is Mode.CMD_LINE && mode.returnTo is Mode.VISUAL) mode.returnTo else mode
    val selectionType = when (effective) {
      is Mode.VISUAL -> effective.selectionType
      is Mode.SELECT -> effective.selectionType
      else -> null
    }
    return selectionType == SelectionType.BLOCK_WISE
  }

  private fun isSelectMode(mode: Mode): Boolean {
    return mode is Mode.SELECT || (mode is Mode.CMD_LINE && mode.returnTo is Mode.SELECT)
  }

  private fun clearFor(ij: Editor) {
    blockBoundsPerEditor.remove(ij)
    clearHighlightersOnly(ij)
  }

  private fun clearHighlightersOnly(ij: Editor) {
    val existing = highlightersPerEditor.remove(ij) ?: return
    for (highlighter in existing) {
      if (highlighter.isValid) ij.markupModel.removeHighlighter(highlighter)
    }
  }

  private fun selectionAttributes(editor: Editor): TextAttributes {
    val scheme = editor.colorsScheme
    return TextAttributes().apply {
      backgroundColor = scheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
    }
  }
}
