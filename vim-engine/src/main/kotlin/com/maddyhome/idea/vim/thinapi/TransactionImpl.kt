/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.scopes.Transaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.common.TextRange

class TransactionImpl(
  private val editor: VimEditor,
  private val context: ExecutionContext,
) : Transaction, ReadImpl(editor, context) {
  override fun deleteText(startOffset: Int, endOffset: Int) {
    editor.deleteString(TextRange(startOffset, endOffset))
  }

  override fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  ) {
    val replaceSequenceSize = endOffset - startOffset - 1
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return
    if (replaceSequenceSize == 0) {
      (editor as MutableVimEditor).insertText(caret, startOffset, text)
    } else {
      val isLastCharInLine = endOffset == editor.getLineEndOffset(caret.getLine(), true) + 1
      val preparedText = if (isLastCharInLine) text.plus("\n") else text
      (editor as MutableVimEditor).replaceString(startOffset, endOffset, preparedText)
    }
  }

  override fun replaceTextBlockwise(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>,
  ) {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return
    val firstLine = editor.offsetToBufferPosition(startOffset).line
    val lastLine = text.size + firstLine - 1

    val startDiff = startOffset - editor.getLineStartOffset(firstLine)
    val endDiff = editor.getLineEndOffset(firstLine, true) - endOffset

    text.zip(firstLine..lastLine).forEach { (lineText, line) ->
      val lineStartOffset = editor.getLineStartOffset(line)
      val lineEndOffset = editor.getLineEndOffset(line, true)

      if (line == firstLine) {
        (editor as MutableVimEditor).replaceString(lineStartOffset + startDiff, lineEndOffset - endDiff, lineText)
      } else {
        (editor as MutableVimEditor).insertText(caret, lineStartOffset + startDiff, lineText)
      }
    }
  }

  override fun updateCaret(caretId: CaretId, info: CaretInfo) {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return
    caret.moveToOffset(info.offset)
    info.selection?.let { (start, end) ->
      caret.setSelection(start, end)
    } ?: caret.removeSelection()
  }
}