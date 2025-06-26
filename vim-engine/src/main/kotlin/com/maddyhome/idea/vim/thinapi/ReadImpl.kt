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
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.caret.CaretRead
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner

open class ReadImpl(
  listenerOwner: ListenerOwner,
  mappingOwner: MappingOwner,
) : Read, VimScopeImpl(listenerOwner, mappingOwner) {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override val textLength: Long
    get() = vimEditor.fileSize()
  override val text: CharSequence
    get() = vimEditor.text()
  override val lineCount: Int
    get() = vimEditor.lineCount()

  override fun <T> forEachCaret(block: CaretRead.() -> T): List<T> {
    return vimEditor.sortedCarets().map { caret -> CaretReadImpl(caret.caretId).block() }
  }

  override fun with(
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

  override fun getLine(offset: Int): Line {
    val lineNumber = vimEditor.offsetToBufferPosition(offset).line
    val lineText = vimEditor.getLineText(lineNumber)
    val lineStartOffset = vimEditor.getLineStartOffset(lineNumber)
    val lineEndOffset = vimEditor.getLineEndOffset(lineNumber)
    return Line(lineNumber, lineText, lineStartOffset, lineEndOffset)
  }

  override val caretData: List<CaretData>
    get() = vimEditor.sortedCarets().map { caret -> caret.caretId to caret.caretInfo }
  override val caretIds: List<CaretId>
    get() = vimEditor.sortedCarets().map { caret -> caret.caretId }

  override fun getGlobalMark(char: Char): Mark? {
    val mark = injector.markService.getGlobalMark(char)
    return mark?.toApiMark()
  }

  override val globalMarks: Set<Mark>
    get() = injector.markService.getAllGlobalMarks().map { it.toApiMark() }.toSet()
}