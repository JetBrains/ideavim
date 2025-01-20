/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.group.ExtensionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType

interface Api {
  fun getRegister(read: Read): Char
  
  // MAP
  fun addMapping(scope: Scope, from: String, to: String, isRecursive: Boolean, vararg modes: Mode)
  fun addMapping(scope: Scope, from: String, vararg modes: Mode, block: ExtensionHandler)
  fun removeMapping(scope: Scope, from: String, vararg modes: Mode)
  
  // OPERATOR FUNCTION
  fun exportOperatorFunction(scope: Scope, name: String, function: (Read) -> Unit)
  fun getOperatorFunctionRange(caretRead: CaretRead): TextRange // see com.intellij.vim.api.plugin.ReplaceWithRegister#getRange
  
  // EDITOR
  fun getMode(scope: Scope): Mode


  // CARET
  fun carets(read: Read): Map<CaretId, CaretInfo>
  fun updateCaret(caretTransaction: CaretTransaction, info: CaretInfo)
  fun getCaretInfo(caretRead: CaretRead): CaretInfo
  fun addCaret(transaction: Transaction, caretInfo: CaretInfo): CaretId
  fun removeCaret(caretTransaction: CaretTransaction)
  
  /**
   * TODO it also sorts caret
   */
  fun forEachCaret(scope: Scope, action: CaretScope.(CaretInfo) -> Unit)
  fun forEachCaretRead(scope: Scope, action: CaretRead.(CaretInfo) -> Unit)
  fun forEachCaretTransaction(scope: Scope, action: CaretTransaction.(CaretInfo) -> Unit)


  // REGISTER
  fun getReg(caretRead: CaretRead, name: Char): String?
  fun getRegType(caretRead: CaretRead, name: Char): SelectionType?
  fun getRegContent(caretRead: CaretRead, name: Char): RegisterContent?
  fun collectTransferableData(caretRead: CaretRead, startOffset: Int, endOffset: Int): TransferableData


  // INSERT
  // TODO what about marks and other side effects?
  // TODO caret placement
  fun insert(caretTransaction: CaretTransaction, register: Char)
  fun insert(caretTransaction: CaretTransaction, text: String, type: SelectionType, transferableData: TransferableData?)


  // DELETE
  fun deleteText(caretTransaction: CaretTransaction, range: TextRange, register: Char? = null)
  fun deleteText(caretTransaction: CaretTransaction, startOffset: Int, endOffset: Int, register: Char? = null)
  
  
  // MARKS
  fun getChangeMarks(caretRead: CaretRead): TextRange?
  fun getVisualMarks(caretRead: CaretRead): TextRange?
  
  
  // ANCHORS
}

enum class Mode {
  NORMAL,
  VISUAL,
  SELECT,
  OP_PENDING,
  INSERT,
  COMMAND,
}

interface TransferableData
interface RegisterContent {
  val text: String
  val type: SelectionType
  val transferableData: TransferableData
}

@JvmInline
value class CaretId(val id: String)
data class CaretInfo(
  val offset: Int,
  val selection: Pair<Int, Int>?,
)

sealed interface TextRange {
  val min: Int
  val max: Int
  
  data class SimpleRange(val start: Int, val end: Int): TextRange {
    override val min: Int = start
    override val max: Int = end
  }
  class BlockRange(starts: IntArray, ends: IntArray): TextRange {
    override val min: Int = starts.min()
    override val max: Int = ends.max()
  }
}