/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.scopes.VimInitPluginScope
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.Transaction
import com.intellij.vim.api.scopes.VimPluginScope


interface VimPluginApi {
  fun getResourceGuard(): ResourceGuard

  fun getRegisterContent(read: Read, caretId: CaretId, register: Char): String?
  fun getCurrentRegisterName(read: Read, caretId: CaretId): Char
  fun getRegisterType(read: Read, caretId: CaretId, register: Char): RegisterType?

  fun addMapping(scope: VimInitPluginScope, fromKeys: String, toKeys: String, isRecursive: Boolean, vararg mode: Mode)
  fun addMapping(
    fromKeys: String,
    scope: VimInitPluginScope,
    isRecursive: Boolean,
    isRepeatable: Boolean,
    action: VimPluginScope.() -> Unit,
    vararg mode: Mode,
  )

  fun removeMapping(scope: VimInitPluginScope, fromKeys: String, vararg mode: Mode)

  fun exportOperatorFunction(name: String, scope: VimInitPluginScope, function: VimPluginScope.() -> Boolean)
  fun setOperatorFunction(scope: VimPluginScope, name: String)
  fun executeNormal(scope: VimPluginScope, command: String)

  fun getMode(scope: VimPluginScope): Mode
  fun getSelectionTypeForCurrentMode(scope: VimPluginScope): TextSelectionType?
  fun exitVisualMode(scope: VimPluginScope)

  fun deleteText(transaction: Transaction, startOffset: Int, endOffset: Int)
  fun replaceText(
    transaction: Transaction,
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  )

  fun replaceTextBlockwise(
    transaction: Transaction,
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>
  )

  fun getChangeMarks(read: Read, caretId: CaretId): Pair<Int, Int>?
  fun getVisualMarks(read: Read, caretId: CaretId): Pair<Int, Int>?

  fun getLineStartOffset(read: Read, line: Int): Int
  fun getLineEndOffset(read: Read, line: Int, allowEnd: Boolean): Int

  fun getAllCaretIds(read: Read): List<CaretId>
  fun getALlCaretIdsSortedByOffset(read: Read): List<CaretId>
  fun getCaretInfo(read: Read, caretId: CaretId): CaretInfo?

  fun getAllCaretsData(read: Read): List<CaretData>
  fun getAllCaretsDataSortedByOffset(read: Read): List<CaretData>

  fun updateCaret(transaction: Transaction, caretId: CaretId, caretInfo: CaretInfo)

  fun getCaretLine(read: Read, caretId: CaretId): Int?
  fun addCaret(transaction: Transaction, caretInfo: CaretInfo): CaretId
  fun removeCaret(transaction: Transaction, caretId: CaretId)

  fun getVimVariableInt(scope: VimPluginScope, vimVariablesScope: VimVariablesScope, name: String): Int?
}
