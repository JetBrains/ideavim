/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.multiplecursors.CaseSensitivity
import com.maddyhome.idea.vim.extension.multiplecursors.selectRegions
import com.maddyhome.idea.vim.extension.multiplecursors.selectedRange
import com.maddyhome.idea.vim.extension.multiplecursors.startOccurrenceSearch
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Turns the Visual selection into regions, which are characterwise selections
 *
 * A linewise selection becomes a region on each line, without the empty lines. A blockwise one becomes a region on each
 * of its lines. Next occurrences are searched for the text of the last region.
 */
internal class VisualAddHandler(private val caseSensitivity: CaseSensitivity) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val area = VisualArea.of(editor) ?: return
    val regions = when (area.type) {
      SelectionType.CHARACTER_WISE -> null
      SelectionType.LINE_WISE -> lineRegions(editor, area)
      SelectionType.BLOCK_WISE -> blockRegions(editor.ij)
    }
    if (regions != null) {
      if (regions.isEmpty()) return
      editor.ij.selectRegions(regions)
    }

    val primaryCaret = editor.ij.caretModel.primaryCaret
    startOccurrenceSearch(editor.ij, primaryCaret.selectedRange, wholeWord = false, caseSensitivity)
  }

  private fun lineRegions(editor: VimEditor, area: VisualArea): List<TextRange> =
    area.lines
      .map { TextRange(editor.getLineStartOffset(it), editor.getLineEndOffset(it)) }
      .filter { it.endOffset > it.startOffset }

  private fun blockRegions(editor: Editor): List<TextRange> =
    editor.caretModel.allCarets.map { it.selectedRange }.filter { it.endOffset > it.startOffset }

}
