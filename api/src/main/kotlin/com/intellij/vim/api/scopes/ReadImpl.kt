/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.VimPluginApi
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor

internal class ReadImpl(
  internal val editor: VimEditor,
  internal val context: ExecutionContext,
  internal val vimPluginApi: VimPluginApi,
): Read {
  override fun getCurrentRegisterName(caretId: CaretId): Char {
    return vimPluginApi.getCurrentRegisterName(editor, caretId)
  }

  override fun getRegisterContent(caretId: CaretId, register: Char): String? {
    return vimPluginApi.getRegisterContent(editor, context, caretId, register)
  }

  override fun getRegisterType(
    caretId: CaretId,
    register: Char,
  ): RegisterType? {
    return vimPluginApi.getRegisterType(editor, context, caretId, register)
  }

  override fun getVisualSelectionMarks(caretId: CaretId): Pair<Int, Int>? {
    return vimPluginApi.getVisualMarks(editor, caretId)
  }

  override fun getChangeMarks(caretId: CaretId): Pair<Int, Int>? {
    return vimPluginApi.getChangeMarks(editor, caretId)
  }

  override fun getCaretLine(caretId: CaretId): Int? {
    return vimPluginApi.getCaretLine(editor, caretId)
  }

  override fun getLineStartOffset(line: Int): Int {
    return vimPluginApi.getLineStartOffset(editor, line)
  }

  override fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return vimPluginApi.getLineEndOffset(editor, line, allowEnd)
  }

  override fun getAllCaretsData(): List<CaretData> {
    return vimPluginApi.getAllCaretsData(editor)
  }

  override fun getAllCaretsDataSortedByOffset(): List<CaretData> {
    return vimPluginApi.getAllCaretsDataSortedByOffset(editor)
  }

  override fun getAllCaretIds(): List<CaretId> {
    return vimPluginApi.getAllCaretIds(editor)
  }

  override fun getAllCaretIdsSortedByOffset(): List<CaretId> {
    return vimPluginApi.getALlCaretIdsSortedByOffset(editor)
  }

  override fun getCaretInfo(caretId: CaretId): CaretInfo? {
    return vimPluginApi.getCaretInfo(editor, caretId)
  }
}

internal fun <T> ReadImpl.executeRead(action: Read.() -> T): T {
  return vimPluginApi.getResourceGuard().read(this, action)
}