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

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimLastSelectionType
import com.maddyhome.idea.vim.helper.vimLastVisualOperatorRange
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.option.OptionsManager

/**
 * @author Alex Plate
 */
class VisualMotionGroup {
  fun selectPreviousVisualMode(editor: Editor): Boolean {
    val lastSelectionType = editor.vimLastSelectionType ?: return false
    val visualMarks = VimPlugin.getMark().getVisualSelectionMarks(editor) ?: return false

    editor.caretModel.removeSecondaryCarets()

    editor.commandState.pushState(CommandState.Mode.VISUAL, lastSelectionType.toSubMode(), MappingMode.VISUAL)

    val primaryCaret = editor.caretModel.primaryCaret
    primaryCaret.vimSetSelection(visualMarks.startOffset, visualMarks.endOffset, true)

    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

    return true
  }

  fun swapVisualSelections(editor: Editor): Boolean {
    val lastSelectionType = editor.vimLastSelectionType ?: return false

    val lastVisualRange = VimPlugin.getMark().getVisualSelectionMarks(editor) ?: return false
    val primaryCaret = editor.caretModel.primaryCaret
    editor.caretModel.removeSecondaryCarets()
    val vimSelectionStart = primaryCaret.vimSelectionStart

    editor.vimLastSelectionType = SelectionType.fromSubMode(editor.subMode)
    VimPlugin.getMark().setVisualSelectionMarks(editor, TextRange(vimSelectionStart, primaryCaret.offset))

    editor.subMode = lastSelectionType.toSubMode()
    primaryCaret.vimSetSelection(lastVisualRange.startOffset, lastVisualRange.endOffset, true)

    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

    return true
  }

  fun swapVisualEnds(editor: Editor, caret: Caret): Boolean {
    val vimSelectionStart = caret.vimSelectionStart
    caret.vimSelectionStart = caret.offset

    MotionGroup.moveCaret(editor, caret, vimSelectionStart)

    return true
  }

  fun swapVisualEndsBigO(editor: Editor): Boolean {
    val caret = editor.caretModel.primaryCaret
    val anotherSideCaret = editor.caretModel.allCarets.let { if (it.first() == caret) it.last() else it.first() }
    val adj = VimPlugin.getVisualMotion().selectionAdj

    if (caret.offset == caret.selectionStart) {
      caret.vimSelectionStart = anotherSideCaret.selectionStart
      MotionGroup.moveCaret(editor, caret, caret.selectionEnd - adj)
    } else {
      caret.vimSelectionStart = anotherSideCaret.selectionEnd - adj
      MotionGroup.moveCaret(editor, caret, caret.selectionStart)
    }

    return true
  }

  //=============================== ENTER VISUAL and SELECT MODE ==============================================

  /**
   * This function toggles visual mode.
   *
   * If visual mode is disabled, enable it
   * If visual mode is enabled, but [subMode] differs, update visual according to new [subMode]
   * If visual mode is enabled with the same [subMode], disable it
   */
  fun toggleVisual(editor: Editor, count: Int, rawCount: Int, subMode: CommandState.SubMode): Boolean {
    if (!editor.inVisualMode) {
      // Enable visual subMode
      if (rawCount > 0) {
        val primarySubMode = editor.caretModel.primaryCaret.vimLastVisualOperatorRange?.type?.toSubMode()
          ?: subMode
        editor.commandState.pushState(CommandState.Mode.VISUAL, primarySubMode, MappingMode.VISUAL)

        editor.vimForEachCaret {
          val range = it.vimLastVisualOperatorRange ?: VisualChange.default(subMode)
          val end = VisualOperation.calculateRange(editor, range, count, it)
          val lastColumn = if (range.columns == MotionGroup.LAST_COLUMN) MotionGroup.LAST_COLUMN else editor.offsetToLogicalPosition(end).column
          it.vimLastColumn = lastColumn
          it.vimSetSelection(it.offset, end, true)
        }
      } else {
        editor.commandState.pushState(CommandState.Mode.VISUAL, subMode, MappingMode.VISUAL)
        editor.vimForEachCaret { it.vimSetSelection(it.offset) }
      }
      return true
    }

    if (subMode == editor.subMode) {
      // Disable visual subMode
      editor.exitVisualMode()
      return true
    }

    // Update visual subMode with new sub subMode
    editor.subMode = subMode
    for (caret in editor.caretModel.allCarets) {
      caret.vimUpdateEditorSelection()
    }

    return true
  }

  @Deprecated("Use enterVisualMode or toggleVisual methods")
  fun setVisualMode(editor: Editor) {
    val autodetectedMode = autodetectVisualSubmode(editor)

    if (editor.inVisualMode) {
      editor.commandState.popState()
    }
    editor.commandState.pushState(CommandState.Mode.VISUAL, autodetectedMode, MappingMode.VISUAL)
    if (autodetectedMode == CommandState.SubMode.VISUAL_BLOCK) {
      val (start, end) = blockModeStartAndEnd(editor)
      editor.caretModel.removeSecondaryCarets()
      editor.caretModel.primaryCaret.vimSetSelection(start, (end - selectionAdj).coerceAtLeast(0), true)
    } else {
      editor.caretModel.allCarets.forEach {
        if (!it.hasSelection()) {
          it.vimSetSelection(it.offset)
          MotionGroup.moveCaret(editor, it, it.offset)
          return@forEach
        }

        val selectionStart = it.selectionStart
        val selectionEnd = it.selectionEnd
        if (selectionStart == it.offset) {
          it.vimSetSelection((selectionEnd - selectionAdj).coerceAtLeast(0), selectionStart, true)
        } else {
          it.vimSetSelection(selectionStart, (selectionEnd - selectionAdj).coerceAtLeast(0), true)
        }
      }
    }
    KeyHandler.getInstance().reset(editor)
  }

  /**
   * Enters visual mode based on current editor state.
   * If [subMode] is null, subMode will be detected automatically
   *
   * it:
   * - Updates command state
   * - Updates [vimSelectionStart] property
   * - Updates caret colors
   * - Updates care shape
   *
   * - DOES NOT change selection
   * - DOES NOT move caret
   * - DOES NOT check if carets actually have any selection
   */
  fun enterVisualMode(editor: Editor, subMode: CommandState.SubMode? = null): Boolean {
    val autodetectedSubMode = subMode ?: autodetectVisualSubmode(editor)
    editor.commandState.pushState(CommandState.Mode.VISUAL, autodetectedSubMode, MappingMode.VISUAL)
    if (autodetectedSubMode == CommandState.SubMode.VISUAL_BLOCK) {
      editor.caretModel.primaryCaret.run { vimSelectionStart = vimLeadSelectionOffset }
    } else {
      editor.caretModel.allCarets.forEach { it.vimSelectionStart = it.vimLeadSelectionOffset }
    }
    updateCaretState(editor)
    return true
  }

  fun enterSelectMode(editor: Editor, subMode: CommandState.SubMode): Boolean {
    editor.commandState.pushState(CommandState.Mode.SELECT, subMode, MappingMode.SELECT)
    editor.vimForEachCaret { it.vimSelectionStart = it.vimLeadSelectionOffset }
    updateCaretState(editor)
    return true
  }

  fun autodetectVisualSubmode(editor: Editor): CommandState.SubMode {
    if (editor.caretModel.caretCount > 1 && seemsLikeBlockMode(editor)) {
      return CommandState.SubMode.VISUAL_BLOCK
    }
    if (editor.caretModel.allCarets.all { caret ->
        // Detect if visual mode is character wise or line wise
        val selectionStart = caret.selectionStart
        val selectionEnd = caret.selectionEnd
        val logicalStartLine = editor.offsetToLogicalPosition(selectionStart).line
        val logicalEnd = editor.offsetToLogicalPosition(selectionEnd)
        val logicalEndLine = if (logicalEnd.column == 0) (logicalEnd.line - 1).coerceAtLeast(0) else logicalEnd.line
        val lineStartOfSelectionStart = EditorHelper.getLineStartOffset(editor, logicalStartLine)
        val lineEndOfSelectionEnd = EditorHelper.getLineEndOffset(editor, logicalEndLine, true)
        lineStartOfSelectionStart == selectionStart && (lineEndOfSelectionEnd + 1 == selectionEnd || lineEndOfSelectionEnd == selectionEnd)
      }) return CommandState.SubMode.VISUAL_LINE
    return CommandState.SubMode.VISUAL_CHARACTER
  }

  private fun seemsLikeBlockMode(editor: Editor): Boolean {
    val selections = editor.caretModel.allCarets.map {
      val adj = if (editor.offsetToLogicalPosition(it.selectionEnd).column == 0) 1 else 0
      it.selectionStart to (it.selectionEnd - adj).coerceAtLeast(0)
    }.sortedBy { it.first }
    val selectionStartColumn = editor.offsetToLogicalPosition(selections.first().first).column
    val selectionStartLine = editor.offsetToLogicalPosition(selections.first().first).line

    val maxColumn = selections.map { editor.offsetToLogicalPosition(it.second).column }.max() ?: return false
    selections.forEachIndexed { i, it ->
      if (editor.offsetToLogicalPosition(it.first).line != editor.offsetToLogicalPosition(it.second).line) {
        return false
      }
      if (editor.offsetToLogicalPosition(it.first).column != selectionStartColumn) {
        return false
      }
      val lineEnd = editor.offsetToLogicalPosition(EditorHelper.getLineEndForOffset(editor, it.second)).column
      if (editor.offsetToLogicalPosition(it.second).column != maxColumn.coerceAtMost(lineEnd)) {
        return false
      }
      if (editor.offsetToLogicalPosition(it.first).line != selectionStartLine + i) {
        return false
      }
    }
    return true
  }

  private fun blockModeStartAndEnd(editor: Editor): Pair<Int, Int> {
    val selections = editor.caretModel.allCarets.map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
    val maxColumn = selections.map { editor.offsetToLogicalPosition(it.second).column }.max()
      ?: throw RuntimeException("No carets")
    val lastLine = editor.offsetToLogicalPosition(selections.last().first).line
    return selections.first().first to editor.logicalPositionToOffset(LogicalPosition(lastLine, maxColumn))
  }

  val exclusiveSelection: Boolean
    get() = OptionsManager.selection.value == "exclusive"
  val selectionAdj: Int
    get() = if (exclusiveSelection) 0 else 1
}
