/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.HighlightId
import com.intellij.vim.api.Jump
import com.intellij.vim.api.scopes.Transaction
import com.intellij.vim.api.scopes.caret.CaretRead
import com.intellij.vim.api.scopes.caret.CaretTransaction
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import kotlin.io.path.pathString
import com.maddyhome.idea.vim.mark.Jump as EngineJump

class TransactionImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : Transaction {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override fun <T> forEachCaret(block: CaretTransaction.() -> T): List<T> {
    return vimEditor.sortedCarets()
      .map { caret -> CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId).block() }
  }

  override fun with(
    caretId: CaretId,
    block: CaretTransaction.() -> Unit,
  ) {
    vimEditor.carets().find { it.id == caretId.id }
      ?.let { caret -> block(CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId)) } ?: return
  }

  override fun withPrimaryCaret(block: CaretTransaction.() -> Unit) {
    block(CaretTransactionImpl(listenerOwner, mappingOwner, vimEditor.primaryCaret().caretId))
  }

  override fun addCaret(offset: Int): CaretId {
    TODO("Not yet implemented")
  }

  override fun removeCaret(caretId: CaretId) {
    TODO("Not yet implemented")
  }

  override fun addHighlight(
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

  override fun removeHighlight(highlightId: HighlightId) {
    injector.highlightingService.removeHighlighter(vimEditor, highlightId)
  }

  override fun setMark(char: Char): Boolean {
    return injector.markService.setMark(vimEditor, char)
  }

  override fun removeMark(char: Char) {
    injector.markService.removeMark(vimEditor, char)
  }

  override fun setGlobalMark(char: Char): Boolean {
    val editor = vimEditor
    val offset = editor.currentCaret().offset
    return injector.markService.setGlobalMark(editor, char, offset)
  }

  override fun removeGlobalMark(char: Char) {
    injector.markService.removeGlobalMark(char)
  }

  override fun setGlobalMark(char: Char, offset: Int): Boolean {
    return injector.markService.setGlobalMark(vimEditor, char, offset)
  }

  override fun resetAllMarks() {
    injector.markService.resetAllMarks()
  }

  override fun addJump(jump: Jump, reset: Boolean) {
    val engineJump = EngineJump(jump.line, jump.col, jump.filepath.javaPath.pathString, jump.filepath.protocol)
    injector.jumpService.addJump(vimEditor.projectId, engineJump, reset)
  }

  override fun removeJump(jump: Jump) {
    val engineJump = EngineJump(jump.line, jump.col, jump.filepath.javaPath.pathString, jump.filepath.protocol)
    injector.jumpService.removeJump(vimEditor.projectId, engineJump)
  }

  override fun dropLastJump() {
    injector.jumpService.dropLastJump(vimEditor.projectId)
  }

  override fun clearJumps() {
    injector.jumpService.clearJumps(vimEditor.projectId)
  }
}