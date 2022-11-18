/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.newapi.IjVimEditor

/**
 * @author Alex Plate
 */

/**
 * Update selection according to new CommandState
 * This method should be used for switching from character to line wise selection and so on
 */
fun VimCaret.vimUpdateEditorSelection() {
  val startOffsetMark = vimSelectionStart
  setVisualSelection(startOffsetMark, offset.point, this)
}

/**
 * This works almost like [Caret.getLeadSelectionOffset], but vim-specific
 */
val Caret.vimLeadSelectionOffset: Int
  get() {
    val caretOffset = offset
    if (hasSelection()) {
      val selectionAdj = VimPlugin.getVisualMotion().selectionAdj
      if (caretOffset != selectionStart && caretOffset != selectionEnd) {
        // Try to check if current selection is tweaked by fold region.
        val foldingModel = editor.foldingModel
        val foldRegion = foldingModel.getCollapsedRegionAtOffset(caretOffset)
        if (foldRegion != null) {
          if (foldRegion.startOffset == selectionStart) {
            return (selectionEnd - selectionAdj).coerceAtLeast(0)
          } else if (foldRegion.endOffset == selectionEnd) {
            return selectionStart
          }
        }
      }

      return if (editor.subMode == VimStateMachine.SubMode.VISUAL_LINE) {
        val selectionStartLine = editor.offsetToLogicalPosition(selectionStart).line
        val caretLine = editor.offsetToLogicalPosition(this.offset).line
        if (caretLine == selectionStartLine) {
          val column = editor.offsetToLogicalPosition(selectionEnd).column
          if (column == 0) (selectionEnd - 1).coerceAtLeast(0) else selectionEnd
        } else selectionStart
      } else if (editor.inBlockSubMode) {
        val selections = editor.caretModel.allCarets.map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
        val pCaret = editor.caretModel.primaryCaret
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

fun moveCaretOneCharLeftFromSelectionEnd(editor: Editor, predictedMode: VimStateMachine.Mode) {
  if (predictedMode != VimStateMachine.Mode.VISUAL) {
    if (!predictedMode.isEndAllowed) {
      editor.caretModel.allCarets.forEach { caret ->
        val lineEnd = IjVimEditor(editor).getLineEndForOffset(caret.offset)
        val lineStart = IjVimEditor(editor).getLineStartForOffset(caret.offset)
        if (caret.offset == lineEnd && lineEnd != lineStart) caret.moveToInlayAwareOffset(caret.offset - 1)
      }
    }
    return
  }
  editor.caretModel.allCarets.forEach { caret ->
    if (caret.hasSelection() && caret.selectionEnd == caret.offset) {
      if (caret.selectionEnd <= 0) return@forEach
      if (IjVimEditor(editor).getLineStartForOffset(caret.selectionEnd - 1) != caret.selectionEnd - 1 &&
        caret.selectionEnd > 1 && editor.document.text[caret.selectionEnd - 1] == '\n'
      ) {
        caret.moveToInlayAwareOffset(caret.selectionEnd - 2)
      } else {
        caret.moveToInlayAwareOffset(caret.selectionEnd - 1)
      }
    }
  }
}
