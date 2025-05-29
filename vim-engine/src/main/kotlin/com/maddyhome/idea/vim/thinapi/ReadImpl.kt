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
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.scopes.Read
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange

open class ReadImpl(
  private val editor: VimEditor,
  private val context: ExecutionContext,
) : Read, VimScopeImpl(editor, context) {
  override fun getCurrentRegisterName(caretId: CaretId): Char {
    val caretCount: Int = editor.carets().size
    val registerGroup = injector.registerGroup

    val lastRegisterChar: Char =
      if (caretCount == 1) registerGroup.currentRegister else registerGroup.getCurrentRegisterForMulticaret()
    return lastRegisterChar
  }

  override fun getRegisterContent(caretId: CaretId, register: Char): String? {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.registerStorage.getRegister(editor, context, register)?.text
  }

  override fun getRegisterType(
    caretId: CaretId,
    register: Char,
  ): RegisterType? {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.registerStorage.getRegister(editor, context, register)?.type?.toRegisterType()
  }

  override fun getVisualSelectionMarks(caretId: CaretId): Pair<Int, Int>? {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return Pair(caret.selectionStart, caret.selectionEnd)
  }

  override fun getChangeMarks(caretId: CaretId): Pair<Int, Int>? {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    val changeMarks: TextRange = injector.markService.getChangeMarks(caret) ?: return null
    return Pair(changeMarks.startOffset, changeMarks.endOffset)
  }

  override fun getCaretLine(caretId: CaretId): Int? {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.getBufferPosition().line
  }

  override fun getLineStartOffset(line: Int): Int {
    return editor.getLineStartOffset(line)
  }

  override fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return editor.getLineEndOffset(line, allowEnd)
  }

  override fun getAllCaretsData(): List<CaretData> {
    return editor.carets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretsDataSortedByOffset(): List<CaretData> {
    return editor.sortedCarets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretIds(): List<CaretId> {
    return editor.carets().map { caret -> caret.caretId }
  }

  override fun getAllCaretIdsSortedByOffset(): List<CaretId> {
    return editor.sortedCarets().map { caret -> caret.caretId }
  }

  override fun getCaretInfo(caretId: CaretId): CaretInfo? {
    val caret: VimCaret = editor.carets().find { it.id == caretId.id } ?: return null
    return caret.caretInfo
  }
}