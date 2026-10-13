/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Editor
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.SelectionInfo
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.getTopLevelEditor

/**
 * A copy of an editor's text and carets, so the regex engine can search it away from the EDT
 *
 * The regex engine reads the text, line offsets and carets of the editor it searches. For the real editor, these are
 * the live document and caret model, which may only be read on the EDT. This snapshot answers those calls from the
 * document's immutable text and from caret state captured when it's created. Options are still read through the real
 * editor.
 *
 * Only use this for searching. Everything not overridden here still goes to the live editor.
 */
internal class IjVimEditorSnapshot private constructor(
  editor: Editor,
  private val snapshotText: CharSequence,
  private val tabSize: Int,
  carets: List<VimCaret>,
) : IjVimEditor(editor) {

  private val snapshotCarets: List<VimCaret> = carets.map { IjVimCaretSnapshot(it, this) }

  // Built on first use, which is on the background thread, rather than while the EDT waits
  private val lineStartOffsets: IntArray by lazy {
    buildList {
      add(0)
      snapshotText.forEachIndexed { index, c -> if (c == '\n') add(index + 1) }
    }.toIntArray()
  }

  override fun text(): CharSequence = snapshotText
  override fun fileSize(): Long = snapshotText.length.toLong()
  override fun nativeLineCount(): Int = lineStartOffsets.size
  override fun getText(left: Int, right: Int): CharSequence = snapshotText.subSequence(left, right)

  override fun getLineStartOffset(line: Int): Int = when {
    line < 0 -> 0
    line >= lineStartOffsets.size -> snapshotText.length
    else -> lineStartOffsets[line]
  }

  // Like Document.getLineEndOffset, the end offset doesn't include the line separator
  override fun getLineEndOffset(line: Int): Int =
    if (line + 1 < lineStartOffsets.size) lineStartOffsets[line + 1] - 1 else snapshotText.length

  override fun getLineRange(line: Int): Pair<Int, Int> = getLineStartOffset(line) to getLineEndOffset(line)

  override fun offsetToBufferPosition(offset: Int): BufferPosition {
    val line = lineOf(offset)
    return BufferPosition(line, columnAt(lineStartOffsets[line], offset.coerceAtMost(snapshotText.length)))
  }

  override fun bufferPositionToOffset(position: BufferPosition): Int {
    val lineEnd = getLineEndOffset(position.line)
    var offset = getLineStartOffset(position.line)
    var column = 0
    while (offset < lineEnd && column < position.column) {
      column = nextColumn(column, snapshotText[offset])
      offset++
    }
    return offset
  }

  private fun lineOf(offset: Int): Int {
    val index = lineStartOffsets.binarySearch(offset)
    return if (index >= 0) index else -index - 2
  }

  private fun columnAt(lineStart: Int, offset: Int): Int =
    (lineStart until offset).fold(0) { column, i -> nextColumn(column, snapshotText[i]) }

  // Logical columns expand tabs, the same as Editor.offsetToLogicalPosition
  private fun nextColumn(column: Int, c: Char): Int = if (c == '\t') (column / tabSize + 1) * tabSize else column + 1

  override fun carets(): List<VimCaret> = snapshotCarets
  override fun nativeCarets(): List<VimCaret> = snapshotCarets
  override fun primaryCaret(): VimCaret = snapshotCarets.first()
  override fun currentCaret(): VimCaret = snapshotCarets.first()

  companion object {
    @RequiresEdt
    fun capture(editor: Editor): IjVimEditorSnapshot {
      val topLevelEditor = editor.getTopLevelEditor()
      return IjVimEditorSnapshot(
        topLevelEditor,
        topLevelEditor.document.immutableCharSequence,
        topLevelEditor.settings.getTabSize(topLevelEditor.project).coerceAtLeast(1),
        IjVimEditor(topLevelEditor).carets(),
      )
    }
  }
}

/**
 * The caret state the regex engine reads, captured on the EDT: the offset for `\%#`, and the selections for `\%V`
 *
 * Marks are not captured. Patterns with mark atoms must not be searched in a snapshot.
 */
private class IjVimCaretSnapshot(caret: VimCaret, override val editor: VimEditor) : VimCaret by caret {
  override val offset: Int = caret.offset
  override val selectionStart: Int = caret.selectionStart
  override val selectionEnd: Int = caret.selectionEnd
  override var lastSelectionInfo: SelectionInfo = caret.lastSelectionInfo.copy()
  private val hasSelection = caret.hasSelection()

  override fun hasSelection(): Boolean = hasSelection
}
