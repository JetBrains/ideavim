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
import com.maddyhome.idea.vim.helper.RWLockLabel
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

  /**
   * Enters Visual mode, ensuring that the caret's selection start offset is correctly set
   *
   * Use this to programmatically enter Visual mode. Note that it does not modify the editor's selection.
   */
  override fun enterVisualMode(editor: VimEditor, selectionType: SelectionType, returnTo: Mode): Boolean {
    editor.mode = Mode.VISUAL(selectionType, returnTo)

    // vimLeadSelectionOffset requires read action
    injector.application.runReadAction {
      if (selectionType == SelectionType.BLOCK_WISE) {
        editor.primaryCaret().run { vimSelectionStart = vimLeadSelectionOffset }
      } else {
        editor.nativeCarets().forEach { it.vimSelectionStart = it.vimLeadSelectionOffset }
      }
    }
    return true
  }

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
   * This function toggles visual mode according to the logic required for `v`, `V` and `<C-V>`
   *
   * This is the implementation for `v`, `V` and `<C-V>`. If you need to enter Visual mode, use [enterVisualMode].
   *
   * * If visual mode is disabled, enable it
   * * If visual mode is enabled, but [selectionType] differs, update visual according to new [selectionType]
   * * If visual mode is enabled with the same [selectionType], disable it
   */
  override fun toggleVisual(
    editor: VimEditor,
    count: Int,
    rawCount: Int,
    selectionType: SelectionType,
    returnTo: Mode?,
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

  override fun toggleSelectVisual(editor: VimEditor) {
    val mode = editor.mode
    if (mode is Mode.VISUAL) {
      val previouslyExclusive = exclusiveSelection
      enterSelectMode(editor, mode.selectionType)
      adjustCaretsForSelectionPolicy(editor, previouslyExclusive)
    } else if (mode is Mode.SELECT) {
      val previouslyExclusive = true  // IdeaVim treats Select as always exclusive
      enterVisualMode(editor, mode.selectionType)
      adjustCaretsForSelectionPolicy(editor, previouslyExclusive)
    }
  }

  /**
   * Move the caret after toggling Visual/Select mode, if this also toggles selection policy (inclusive/exclusive)
   *
   * Toggling Visual/Select mode does not update the caret position in Vim. This is because the inclusive/exclusive
   * state of the selection does not change. It is the value of the `'selection'` option, and the same for Visual and
   * Select.
   *
   * IdeaVim can have a different selection policy for Visual and Select; it is more intuitive for Select mode to use
   * "exclusive", but more familiar for Visual to be inclusive (see [VimEditor.isSelectionExclusive] for more
   * background). IdeaVim will therefore try to update the caret position to match what the selection would be if it
   * has just been created with the new selection policy, rather than toggled after the selection was made.
   *
   * In other words, Vim places the caret at the end of the selection when creating an inclusive selection, and
   * _after_ the end of selection when creating an exclusive selection, and IdeaVim updates to match this.
   *
   * Specifically, when switching from inclusive to exclusive, and the caret is at the end of the selection, the caret
   * is moved to be after the selection. And when switching from exclusive to inclusive and the caret is after the end
   * of the selection, it is moved to be at the end of selection.
   */
  private fun adjustCaretsForSelectionPolicy(editor: VimEditor, previouslyExclusive: Boolean) {
    val mode = editor.mode
    // TODO: Improve handling of this.exclusiveSelection
    val isSelectionExclusive = this.exclusiveSelection || mode is Mode.SELECT
    if (mode.selectionType != SelectionType.LINE_WISE) {
      // Remember that VimCaret.selectionEnd is exclusive!
      if (isSelectionExclusive && !previouslyExclusive) {
        // Inclusive -> exclusive
        editor.nativeCarets().forEach {
          if (it.offset == it.selectionEnd - 1) {
            it.moveToInlayAwareOffset(it.selectionEnd) // Caret is on the selection end, move to after
          }
        }
      } else if (!isSelectionExclusive && previouslyExclusive) {
        // Exclusive -> inclusive
        editor.nativeCarets().forEach {
          // If caret offset matches the exclusive selection end offset, then it's positioned after the selection, so
          // move it to the actual end of the selection. Make sure there's enough room on this line to do so
          if (it.offset == it.selectionEnd && it.visualLineStart < it.offset) {
            it.moveToInlayAwareOffset(it.selectionEnd - 1)
          }
        }
      }
    }
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
      // We can't use toggleSelectVisual because when toggling between Select and Visual, we replace the current mode,
      // but here we want to "push" Visual, so we return to Select.
      val previouslyExclusive = true  // Select is always exclusive
      editor.mode = Mode.VISUAL(mode.selectionType, returnTo = mode)
      adjustCaretsForSelectionPolicy(editor, previouslyExclusive)
    } else if (mode is Mode.VISUAL && mode.isSelectPending) {
      // We can use toggleSelectVisual because we're replacing the "pushed" Visual mode with a simple Select, which is
      // the same as when we toggle from Visual to Select
      toggleSelectVisual(editor)
    }
  }

  @RWLockLabel.Readonly
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
}
