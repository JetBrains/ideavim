/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.VisualOperation
import com.maddyhome.idea.vim.group.visual.vimLeadSelectionOffset
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.group.visual.vimUpdateEditorSelection
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.pushVisualMode
import com.maddyhome.idea.vim.helper.setSelectMode
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnTo
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.returnTo
import com.maddyhome.idea.vim.state.mode.selectionType

abstract class VimVisualMotionGroupBase : VimVisualMotionGroup {
  override val exclusiveSelection: Boolean
    get() = injector.globalOptions().selection.contains("exclusive")
  override val selectionAdj: Int
    get() = if (exclusiveSelection) 0 else 1

  override fun enterSelectMode(editor: VimEditor, subMode: SelectionType): Boolean {
    editor.setSelectMode(subMode)
    editor.forEachCaret { it.vimSelectionStart = it.vimLeadSelectionOffset }
    return true
  }

  /**
   * This function toggles visual mode.
   *
   * If visual mode is disabled, enable it
   * If visual mode is enabled, but [selectionType] differs, update visual according to new [selectionType]
   * If visual mode is enabled with the same [selectionType], disable it
   */
  override fun toggleVisual(
    editor: VimEditor,
    count: Int,
    rawCount: Int,
    selectionType: SelectionType,
    returnTo: ReturnTo?
  ): Boolean {
    if (!editor.inVisualMode) {
      // Enable visual subMode
      if (rawCount > 0) {
        val primarySubMode = editor.primaryCaret().vimLastVisualOperatorRange?.type ?: selectionType
        editor.pushVisualMode(primarySubMode)

        editor.forEachCaret {
          val range = it.vimLastVisualOperatorRange ?: VisualChange.default(selectionType)
          val end = VisualOperation.calculateRange(editor, range, count, it)
          val intendedColumn = if (range.columns == VimMotionGroupBase.LAST_COLUMN) {
            VimMotionGroupBase.LAST_COLUMN
          } else {
            editor.offsetToBufferPosition(end).column
          }
          // Set the intended column before moving the caret, then reset because we've moved the caret
          it.vimLastColumn = intendedColumn
          it.vimSetSelection(it.offset, end, true)
          it.vimLastColumn = intendedColumn
        }
      } else {
        editor.mode = Mode.VISUAL(
          selectionType,
          returnTo ?: editor.mode.returnTo
        )
        editor.forEachCaret { it.vimSetSelection(it.offset) }
      }
      return true
    }

    if (selectionType == editor.mode.selectionType) {
      // Disable visual subMode
      editor.exitVisualMode()
      return true
    }

    val mode = editor.mode
    check(mode is Mode.VISUAL)

    // Update visual subMode with new sub subMode
    editor.mode = mode.copy(selectionType = selectionType)
    for (caret in editor.carets()) {
      if (!caret.isValid) continue
      caret.vimUpdateEditorSelection()
    }

    return true
  }

  protected fun seemsLikeBlockMode(editor: VimEditor): Boolean {
    val selections = editor.nativeCarets().map {
      val adj = if (editor.offsetToBufferPosition(it.selectionEnd).column == 0) 1 else 0
      it.selectionStart to (it.selectionEnd - adj).coerceAtLeast(0)
    }.sortedBy { it.first }
    val selectionStartColumn = editor.offsetToBufferPosition(selections.first().first).column
    val selectionStartLine = editor.offsetToBufferPosition(selections.first().first).line

    val maxColumn = selections.maxOfOrNull { editor.offsetToBufferPosition(it.second).column } ?: return false
    selections.forEachIndexed { i, it ->
      if (editor.offsetToBufferPosition(it.first).line != editor.offsetToBufferPosition(it.second).line) {
        return false
      }
      if (editor.offsetToBufferPosition(it.first).column != selectionStartColumn) {
        return false
      }
      val lineEnd =
        editor.offsetToBufferPosition(editor.getLineEndForOffset(it.second)).column
      if (editor.offsetToBufferPosition(it.second).column != maxColumn.coerceAtMost(lineEnd)) {
        return false
      }
      if (editor.offsetToBufferPosition(it.first).line != selectionStartLine + i) {
        return false
      }
    }
    return true
  }

  override fun autodetectVisualSubmode(editor: VimEditor): SelectionType {
    if (editor.carets().size > 1 && seemsLikeBlockMode(editor)) {
      return SelectionType.BLOCK_WISE
    }
    val all = editor.nativeCarets().all { caret ->
      // Detect if visual mode is character wise or line wise
      val selectionStart = caret.selectionStart
      val selectionEnd = caret.selectionEnd
      val startLine = editor.offsetToBufferPosition(selectionStart).line
      val endPosition = editor.offsetToBufferPosition(selectionEnd)
      val endLine = if (endPosition.column == 0) (endPosition.line - 1).coerceAtLeast(0) else endPosition.line
      val lineStartOfSelectionStart = editor.getLineStartOffset(startLine)
      val lineEndOfSelectionEnd = editor.getLineEndOffset(endLine, true)
      lineStartOfSelectionStart == selectionStart && (lineEndOfSelectionEnd + 1 == selectionEnd || lineEndOfSelectionEnd == selectionEnd)
    }
    if (all) return SelectionType.LINE_WISE
    return SelectionType.CHARACTER_WISE
  }

  /**
   * Enters visual mode based on current editor state.
   * If [subMode] is null, subMode will be detected automatically
   *
   * it:
   * - Updates command state
   * - Updates [ImmutableVimCaret.vimSelectionStart] property
   * - Updates caret colors
   * - Updates care shape
   *
   * - DOES NOT change selection
   * - DOES NOT move caret
   * - DOES NOT check if carets actually have any selection
   */
  override fun enterVisualMode(editor: VimEditor, subMode: SelectionType?): Boolean {
    val autodetectedSubMode = subMode ?: autodetectVisualSubmode(editor)
    editor.mode = Mode.VISUAL(autodetectedSubMode)
    //    editor.vimStateMachine.setMode(VimStateMachine.Mode.VISUAL, autodetectedSubMode)
    if (autodetectedSubMode == SelectionType.BLOCK_WISE) {
      editor.primaryCaret().run { vimSelectionStart = vimLeadSelectionOffset }
    } else {
      editor.nativeCarets().forEach { it.vimSelectionStart = it.vimLeadSelectionOffset }
    }
    return true
  }
}
