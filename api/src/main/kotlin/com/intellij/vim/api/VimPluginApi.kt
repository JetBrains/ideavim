/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.scopes.vim.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor


interface VimPluginApi {
  fun getResourceGuard(): ResourceGuard

  fun getRegisterContent(
    editor: VimEditor,
    context: ExecutionContext,
    caretId: CaretId,
    register: Char,
  ): String?

  fun getCurrentRegisterName(
    editor: VimEditor,
    caretId: CaretId,
  ): Char

  fun getRegisterType(
    editor: VimEditor,
    context: ExecutionContext,
    caretId: CaretId,
    register: Char,
  ): RegisterType?

  fun addMapping(fromKeys: String, toKeys: String, isRecursive: Boolean, vararg mode: Mode)
  fun addMapping(
    vimPluginApi: VimPluginApi,
    fromKeys: String,
    isRecursive: Boolean,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
    vararg mode: Mode,
  )

  fun removeMapping(fromKeys: String, vararg mode: Mode)

  fun exportOperatorFunction(name: String, vimPluginApi: VimPluginApi, function: VimScope.() -> Boolean)
  fun setOperatorFunction(name: String)
  fun executeNormal(editor: VimEditor, command: String)

  fun getMode(editor: VimEditor): Mode
  fun getSelectionTypeForCurrentMode(editor: VimEditor): TextSelectionType?
  fun exitVisualMode(editor: VimEditor)

  fun deleteText(editor: VimEditor, startOffset: Int, endOffset: Int)
  fun replaceText(
    editor: VimEditor,
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  )

  fun replaceTextBlockwise(
    editor: VimEditor,
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>,
  )

  fun getChangeMarks(editor: VimEditor, caretId: CaretId): Pair<Int, Int>?
  fun getVisualMarks(editor: VimEditor, caretId: CaretId): Pair<Int, Int>?

  fun getLineStartOffset(editor: VimEditor, line: Int): Int
  fun getLineEndOffset(editor: VimEditor, line: Int, allowEnd: Boolean): Int

  fun getAllCaretIds(editor: VimEditor): List<CaretId>
  fun getALlCaretIdsSortedByOffset(editor: VimEditor): List<CaretId>
  fun getCaretInfo(editor: VimEditor, caretId: CaretId): CaretInfo?

  fun getAllCaretsData(editor: VimEditor): List<CaretData>
  fun getAllCaretsDataSortedByOffset(editor: VimEditor): List<CaretData>

  fun updateCaret(editor: VimEditor, caretId: CaretId, caretInfo: CaretInfo)

  fun getCaretLine(editor: VimEditor, caretId: CaretId): Int?
  fun addCaret(editor: VimEditor, caretInfo: CaretInfo): CaretId
  fun removeCaret(editor: VimEditor, caretId: CaretId)

  fun getVariableInt(editor: VimEditor, context: ExecutionContext, name: String): Int?
}
