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
import com.maddyhome.idea.vim.extension.multiplecursors.OccurrenceSearch
import com.maddyhome.idea.vim.extension.multiplecursors.occurrenceSearch
import com.maddyhome.idea.vim.extension.multiplecursors.selectRegions
import com.maddyhome.idea.vim.extension.multiplecursors.selectedRange
import com.maddyhome.idea.vim.helper.userData
import com.maddyhome.idea.vim.newapi.ij

/**
 * The regions of the last session, by offset like in vim-visual-multi, so they are wrong after the text has changed
 */
private class LastRegions(val regions: List<TextRange>, val search: OccurrenceSearch?)

private var Editor.vimVisualMultiLastRegions: LastRegions? by userData()

internal fun saveLastRegions(editor: VimEditor) {
  val regions = editor.ij.caretModel.allCarets.map { it.selectedRange }.filter { it.endOffset > it.startOffset }
  if (regions.isNotEmpty()) {
    editor.ij.vimVisualMultiLastRegions = LastRegions(regions, editor.ij.occurrenceSearch)
  }
}

/**
 * Selects the regions of the last session again, and continues its search
 */
internal class ReselectLastHandler : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val last = editor.ij.vimVisualMultiLastRegions ?: return
    val textLength = editor.ij.document.textLength
    val regions = last.regions.filter { it.endOffset <= textLength }
    if (regions.isEmpty()) return

    editor.ij.selectRegions(regions)
    editor.ij.occurrenceSearch = last.search
  }
}
