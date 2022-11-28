/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.subMode

/**
 * This works almost like [Caret.getLeadSelectionOffset] in IJ, but vim-specific
 */
val VimCaret.vimLeadSelectionOffset: Int
  get() {
    val caretOffset = offset.point
    if (hasSelection()) {
      val selectionAdj = injector.visualMotionGroup.selectionAdj
      if (caretOffset != selectionStart && caretOffset != selectionEnd) {
        // Try to check if current selection is tweaked by fold region.
        val foldRegion = editor.getCollapsedRegionAtOffset(caretOffset)
        if (foldRegion != null) {
          if (foldRegion.startOffset == selectionStart) {
            return (selectionEnd - selectionAdj).coerceAtLeast(0)
          } else if (foldRegion.endOffset == selectionEnd) {
            return selectionStart
          }
        }
      }

      return if (editor.subMode == VimStateMachine.SubMode.VISUAL_LINE) {
        val selectionStartLine = editor.offsetToBufferPosition(selectionStart).line
        val caretLine = editor.offsetToBufferPosition(this.offset.point).line
        if (caretLine == selectionStartLine) {
          val column = editor.offsetToBufferPosition(selectionEnd).column
          if (column == 0) (selectionEnd - 1).coerceAtLeast(0) else selectionEnd
        } else selectionStart
      } else if (editor.inBlockSubMode) {
        val selections = editor.nativeCarets().map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
        val pCaret = editor.primaryCaret()
        when (pCaret.offset.point) {
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