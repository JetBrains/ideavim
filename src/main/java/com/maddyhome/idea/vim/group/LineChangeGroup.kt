/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.undo.LineChange

/**
 * IntelliJ implementation of [LineChange] — Vim's "U" command ("undo line").
 *
 * The saved line (Vim's `b_u_line_ptr` / `b_u_line_lnum` / `b_u_line_colnr`) is stored as user data
 * on the `Document`, so it lives and dies with the document and is naturally per-buffer.
 *
 * Snapshotting is driven by the document-change listener (see `VimListenerManager.VimDocumentListener`),
 * which figures out which line is about to change and calls [snapshotLine] before the edit is applied.
 */
object LineChangeGroup : LineChange {
  private data class LineSnapshot(val line: Int, val text: String, val col: Int)

  private val SNAPSHOT_KEY = Key.create<LineSnapshot>("IdeaVim.LineChangeSnapshot")

  override fun snapshotLine(line: Int, editor: VimEditor): Boolean {
    val doc = editor.ij.document
    if (line < 0 || line >= doc.lineCount) return false

    val existing = doc.getUserData(SNAPSHOT_KEY)
    if (existing != null && existing.line == line) {
      // Line is already saved: keep the pre-first-change copy (u_saveline's early return).
      return false
    }

    val lineStart = doc.getLineStartOffset(line)
    val lineEnd = doc.getLineEndOffset(line)
    val text = doc.getText(TextRange(lineStart, lineEnd))
    doc.putUserData(SNAPSHOT_KEY, LineSnapshot(line, text, caretColumn(editor, line, lineStart)))
    return true
  }

  override fun undoLineChange(editor: VimEditor, context: ExecutionContext): Boolean {
    val ijEditor = editor.ij
    val doc = ijEditor.document
    val snapshot = doc.getUserData(SNAPSHOT_KEY) ?: return false
    if (snapshot.line >= doc.lineCount) return false

    val lineStart = doc.getLineStartOffset(snapshot.line)
    val lineEnd = doc.getLineEndOffset(snapshot.line)
    val currentText = doc.getText(TextRange(lineStart, lineEnd))
    val currentCol = caretColumn(editor, snapshot.line, lineStart)

    WriteCommandAction.runWriteCommandAction(ijEditor.project) {
      doc.replaceString(lineStart, lineEnd, snapshot.text)
    }

    // Store what we just replaced so a second `U` toggles back ("U can be undone with the next U").
    doc.putUserData(SNAPSHOT_KEY, LineSnapshot(snapshot.line, currentText, currentCol))

    val newLineStart = doc.getLineStartOffset(snapshot.line)
    val newLineEnd = doc.getLineEndOffset(snapshot.line)
    ijEditor.caretModel.primaryCaret.moveToOffset((newLineStart + snapshot.col).coerceIn(newLineStart, newLineEnd))
    return true
  }

  private fun caretColumn(editor: VimEditor, line: Int, lineStart: Int): Int {
    val doc = editor.ij.document
    val caretOffset = editor.ij.caretModel.primaryCaret.offset
    return if (doc.getLineNumber(caretOffset) == line) caretOffset - lineStart else 0
  }
}
