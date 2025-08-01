/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.HighlightId
import com.intellij.vim.api.Jump
import com.intellij.vim.api.scopes.editor.Read
import com.intellij.vim.api.scopes.editor.Transaction
import com.intellij.vim.api.scopes.editor.caret.CaretTransaction
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.thinapi.caretId
import com.maddyhome.idea.vim.thinapi.editor.caret.CaretTransactionImpl
import com.maddyhome.idea.vim.thinapi.getFilePath
import com.maddyhome.idea.vim.mark.Jump as EngineJump

class TransactionImpl : Transaction, Read by ReadImpl() {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override suspend fun <T> forEachCaret(block: suspend CaretTransaction.() -> T): List<T> {
    return vimEditor.sortedCarets()
      .map { caret -> CaretTransactionImpl(caret.caretId).block() }
  }

  override suspend fun <T> with(
    caretId: CaretId,
    block: suspend CaretTransaction.() -> T,
  ): T {
    return CaretTransactionImpl(caretId).block()
  }

  override suspend fun <T> withPrimaryCaret(block: suspend CaretTransaction.() -> T): T {
    return CaretTransactionImpl(vimEditor.primaryCaret().caretId).block()
  }

  override suspend fun addCaret(offset: Int): CaretId? {
    val fileLength = vimEditor.text().length
    val validRange = 0 until fileLength
    if (offset !in validRange) {
      throw IllegalArgumentException("Offset $offset is not in valid range $validRange")
    }

    return vimEditor.addCaret(offset)?.caretId
  }

  override suspend fun removeCaret(caretId: CaretId) {
    val caret = vimEditor.carets().find { it.id == caretId.id }
      ?: throw IllegalArgumentException("Caret with id $caretId not found")

    vimEditor.removeCaret(caret)
  }

  override suspend fun addHighlight(
    startOffset: Int,
    endOffset: Int,
    backgroundColor: Color?,
    foregroundColor: Color?,
  ): HighlightId {
    return injector.highlightingService.addHighlighter(
      vimEditor,
      startOffset,
      endOffset,
      backgroundColor,
      foregroundColor
    )
  }

  override suspend fun removeHighlight(highlightId: HighlightId) {
    injector.highlightingService.removeHighlighter(vimEditor, highlightId)
  }

  override suspend fun setMark(char: Char): Boolean {
    return injector.markService.setMark(vimEditor, char)
  }

  override suspend fun removeMark(char: Char) {
    injector.markService.removeMark(vimEditor, char)
  }

  override suspend fun setGlobalMark(char: Char): Boolean {
    val editor = vimEditor
    val offset = editor.currentCaret().offset
    return injector.markService.setGlobalMark(editor, char, offset)
  }

  override suspend fun removeGlobalMark(char: Char) {
    injector.markService.removeGlobalMark(char)
  }

  override suspend fun setGlobalMark(char: Char, offset: Int): Boolean {
    return injector.markService.setGlobalMark(vimEditor, char, offset)
  }

  override suspend fun resetAllMarks() {
    injector.markService.resetAllMarks()
  }

  override suspend fun addJump(jump: Jump, reset: Boolean) {
    val protocol = jump.filepath.protocol
    val filePath = jump.filepath.getFilePath()
    val engineJump = EngineJump(jump.line, jump.col, filePath, protocol)
    injector.jumpService.addJump(vimEditor.projectId, engineJump, reset)
  }

  override suspend fun removeJump(jump: Jump) {
    val protocol = jump.filepath.protocol
    val filePath = jump.filepath.getFilePath()
    val engineJump = EngineJump(jump.line, jump.col, filePath, protocol)
    injector.jumpService.removeJump(vimEditor.projectId, engineJump)
  }

  override suspend fun dropLastJump() {
    injector.jumpService.dropLastJump(vimEditor.projectId)
  }

  override suspend fun clearJumps() {
    injector.jumpService.clearJumps(vimEditor.projectId)
  }
}