/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.caret.CaretRead
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner

open class ReadImpl(
  listenerOwner: ListenerOwner,
  mappingOwner: MappingOwner,
) : Read, VimScopeImpl(listenerOwner, mappingOwner) {
  private val vimEditor: VimEditor
    get() = injector.editorService.getFocusedEditor()!!

  override val size: Long
    get() = vimEditor.fileSize()
  override val text: CharSequence
    get() = vimEditor.text()
  override val lineCount: Int
    get() = vimEditor.lineCount()

  override fun forEachCaret(block: CaretRead.() -> Unit) {
    vimEditor.carets().forEach { caret -> CaretReadImpl(caret.caretId).block() }
  }

  override fun <T> mapEachCaret(block: CaretRead.() -> T): List<T> {
    return vimEditor.carets().map { caret -> CaretReadImpl(caret.caretId).block() }
  }

  override fun forEachCaretSorted(block: CaretRead.() -> Unit) {
    vimEditor.sortedCarets().forEach { caret -> CaretReadImpl(caret.caretId).block() }
  }

  override fun withCaret(
    caretId: CaretId,
    block: CaretRead.() -> Unit,
  ) {
    CaretReadImpl(caretId).block()
  }

  override fun getLineStartOffset(line: Int): Int {
    return vimEditor.getLineStartOffset(line)
  }

  override fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return vimEditor.getLineEndOffset(line, allowEnd)
  }

  override fun getText(startOffset: Int, endOffset: Int): CharSequence {
    return vimEditor.getText(startOffset, endOffset)
  }

  override fun getAllCaretsData(): List<CaretData> {
    return vimEditor.carets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretsDataSortedByOffset(): List<CaretData> {
    return vimEditor.sortedCarets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretIds(): List<CaretId> {
    return vimEditor.carets().map { caret -> caret.caretId }
  }

  override fun getAllCaretIdsSortedByOffset(): List<CaretId> {
    return vimEditor.sortedCarets().map { caret -> caret.caretId }
  }
}