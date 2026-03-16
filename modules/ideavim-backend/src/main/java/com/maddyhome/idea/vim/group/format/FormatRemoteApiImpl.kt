/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.format

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [FormatRemoteApi].
 *
 * Sets selection on the backend editor and executes the platform's auto-indent action.
 * Because this runs on the backend, [CommandProcessor] groups all document modifications
 * as a single undo step.
 *
 * Multiple ranges are processed bottom-to-top so that formatting one range does not
 * shift the line numbers of ranges above it.
 *
 * Caret positioning after formatting is handled by the caller (vim-engine layer),
 * not by this backend handler, so that each caller can position the correct caret
 * (important for per-caret handlers like `==`).
 */
class FormatRemoteApiImpl : FormatRemoteApi {
  override suspend fun format(
    editorId: EditorId,
    startOffsets: List<Int>,
    endOffsets: List<Int>,
  ) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val document = editor.document

    // Convert offsets to line ranges, adjusting endLine when it falls exactly on a line start
    val lineRanges = getLineRanges(startOffsets, endOffsets, document)

    // Sort ranges by startLine descending so we format bottom-to-top.
    // This ensures formatting a later range doesn't shift line numbers of earlier ranges.
    val sortedRanges = lineRanges.sortedByDescending { it.first }

    CommandProcessor.getInstance().executeCommand(editor.project, {
      val action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES)
      for ((startLine, endLine) in sortedRanges) {
        val selStart = document.getLineStartOffset(startLine)
        val selEnd = document.getLineEndOffset(endLine)
        editor.selectionModel.setSelection(selStart, selEnd)
        ActionManager.getInstance().tryToExecute(action, null, editor.contentComponent, "IdeaVim", true).waitFor(5_000)
      }
      editor.selectionModel.removeSelection()
    }, "AutoIndent", null)
  }

  private fun getLineRanges(
    startOffsets: List<Int>,
    endOffsets: List<Int>,
    document: Document,
  ): List<Pair<Int, Int>> = startOffsets.zip(endOffsets).map { (start, end) ->
    val startLine = document.getLineNumber(start)
    var endLine = document.getLineNumber(end)
    if (endLine > startLine && document.getLineStartOffset(endLine) == end) {
      endLine--
    }
    startLine to endLine
  }
}