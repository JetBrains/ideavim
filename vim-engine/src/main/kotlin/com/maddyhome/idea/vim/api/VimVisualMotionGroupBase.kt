/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.motion.select.SelectToggleVisualMode
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.VisualOperation
import com.maddyhome.idea.vim.group.visual.vimLeadSelectionOffset
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.group.visual.vimUpdateEditorSelection
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

abstract class VimVisualMotionGroupBase : VimVisualMotionGroup {
  override val exclusiveSelection: Boolean
    get() = injector.globalOptions().selection.contains("exclusive")
  override val selectionAdj: Int
    get() = if (exclusiveSelection) 0 else 1

  override fun enterSelectMode(editor: VimEditor, selectionType: SelectionType): Boolean {
    // If we're already in Select or toggling from Visual, replace the current mode (keep the existing returnTo),
    // otherwise push Select, using the current mode as returnTo.
    // If we're entering from Normal, use its own returnTo, as this will handle both Normal and "Internal Normal".
    // And return back to Select if we were originally in Select and entered Visual for a single command (eg `gh<C-O>e`)
    val mode = editor.mode
    editor.mode = when {
      mode is Mode.VISUAL && mode.isSelectPending -> mode.returnTo
      mode is Mode.VISUAL || mode is Mode.SELECT -> Mode.SELECT(selectionType, mode.returnTo)
      mode is Mode.NORMAL -> Mode.SELECT(selectionType, mode.returnTo)
      else -> Mode.SELECT(selectionType, mode)
    }
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
    returnTo: Mode?
  ): Boolean {
    if (!editor.inVisualMode) {
      if (rawCount > 0) {
        val primarySelectionType = editor.primaryCaret().vimLastVisualOperatorRange?.type ?: selectionType
        editor.mode = Mode.VISUAL(primarySelectionType, editor.mode.returnTo)

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
        editor.mode = Mode.VISUAL(selectionType, returnTo ?: editor.mode.returnTo)
        editor.forEachCaret { it.vimSetSelection(it.offset) }
      }
      return true
    }

    if (selectionType == editor.mode.selectionType) {
      editor.exitVisualMode()
      return true
    }

    val mode = editor.mode
    check(mode is Mode.VISUAL)

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

  override fun detectSelectionType(editor: VimEditor): SelectionType {
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
   *
   * If [selectionType] is null, it will be detected automatically
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
  override fun enterVisualMode(editor: VimEditor, selectionType: SelectionType?): Boolean {
    val newSelectionType = selectionType ?: detectSelectionType(editor)

    editor.mode = Mode.VISUAL(newSelectionType)

    // vimLeadSelectionOffset requires read action
    injector.application.runReadAction {
      if (newSelectionType == SelectionType.BLOCK_WISE) {
        editor.primaryCaret().run { vimSelectionStart = vimLeadSelectionOffset }
      } else {
        editor.nativeCarets().forEach { it.vimSelectionStart = it.vimLeadSelectionOffset }
      }
    }
    return true
  }

  /**
   * When in Select mode, enter Visual mode for a single command
   *
   * While the Vim docs state that this is for the duration of a single Visual command, it also includes motions. This
   * is different to "Insert Visual" mode (`i<C-O>v`) which allows multiple motions until an operator is invoked.
   *
   * If already in Visual, this function will return to Select.
   *
   * See `:help v_CTRL-O`.
   */
  override fun processSingleVisualCommand(editor: VimEditor) {
    val mode = editor.mode
    if (mode is Mode.SELECT) {
      editor.mode = Mode.VISUAL(mode.selectionType, returnTo = mode)
      // TODO: This is a copy of code from SelectToggleVisualMode.toggleMode. It should be moved to VimVisualMotionGroup
      // IdeaVim always treats Select mode as exclusive. This will adjust the caret from exclusive to (potentially)
      // inclusive, depending on the value of 'selection'
      if (mode.selectionType != SelectionType.LINE_WISE) {
        editor.nativeCarets().forEach {
          if (it.offset == it.selectionEnd && it.visualLineStart <= it.offset - injector.visualMotionGroup.selectionAdj) {
            it.moveToInlayAwareOffset(it.offset - injector.visualMotionGroup.selectionAdj)
          }
        }
      }
    }
    else if (mode is Mode.VISUAL && mode.isSelectPending) {
      // TODO: It would be better to move this to VimVisualMotionGroup
      SelectToggleVisualMode.toggleMode(editor)
    }
  }
}
