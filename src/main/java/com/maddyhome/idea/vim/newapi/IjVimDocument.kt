/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.maddyhome.idea.vim.api.VimDocument
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.LiveRange

class IjVimDocument(val document: Document) : VimDocument {

  private val changeListenersMap: MutableMap<ChangesListener, DocumentListener> = mutableMapOf()

  override fun addChangeListener(listener: ChangesListener) {
    val nativeListener = object : DocumentListener {
      private var virtualSpaceFill = 0

      override fun beforeDocumentChange(event: DocumentEvent) {
        virtualSpaceFill = getVirtualSpaceFill(event)
      }

      override fun documentChanged(event: DocumentEvent) {
        listener.documentChanged(
          ChangesListener.Change(
            event.oldFragment.toString(),
            event.newFragment.toString(),
            event.offset,
            virtualSpaceFill,
          ),
        )
      }
    }
    changeListenersMap[listener] = nativeListener
    document.addDocumentListener(nativeListener)
  }

  /**
   * Text inserted at the end of a line while a caret is placed after it starts with the whitespace that fills the space
   * up to the caret. Returns the length of that whitespace
   */
  private fun getVirtualSpaceFill(event: DocumentEvent): Int {
    if (event.oldLength != 0 || event.newLength == 0) return 0
    for (editor in EditorFactory.getInstance().getEditors(document)) {
      if (!editor.settings.isVirtualSpace) continue
      for (caret in editor.caretModel.allCarets) {
        val position = caret.logicalPosition
        if (position.line >= document.lineCount || document.getLineEndOffset(position.line) != event.offset) continue
        val lineEndColumn = editor.offsetToLogicalPosition(event.offset).column
        if (position.column <= lineEndColumn) continue
        val tabSize = editor.settings.getTabSize(editor.project)
        return getFillLength(event.newFragment, lineEndColumn, position.column, tabSize)
      }
    }
    return 0
  }

  private fun getFillLength(text: CharSequence, fromColumn: Int, toColumn: Int, tabSize: Int): Int {
    var column = fromColumn
    var length = 0
    while (column < toColumn && length < text.length) {
      column = when (text[length]) {
        ' ' -> column + 1
        '\t' -> (column / tabSize + 1) * tabSize
        else -> return length
      }
      length++
    }
    return length
  }

  override fun removeChangeListener(listener: ChangesListener) {
    val nativeListener = changeListenersMap.remove(listener) ?: error("Existing listener expected")
    document.removeDocumentListener(nativeListener)
  }

  override fun getOffsetGuard(offset: Int): LiveRange? {
    return document.getOffsetGuard(offset)?.vim
  }

  override fun getRangeGuard(start: Int, end: Int): LiveRange? {
    return document.getRangeGuard(start, end)?.vim
  }
}
