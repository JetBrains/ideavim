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

interface Read : VimScope

fun Read.getCurrentRegisterName(caretId: CaretId): Char {
  return vimPluginApi.getCurrentRegisterName(editor, caretId)
}

fun Read.getRegisterContent(caretId: CaretId, register: Char): String? {
  return vimPluginApi.getRegisterContent(editor, context, caretId, register)
}

fun Read.getRegisterType(caretId: CaretId, register: Char): RegisterType? {
  return vimPluginApi.getRegisterType(editor, context, caretId, register)
}

fun Read.getVisualSelectionMarks(caretId: CaretId): Pair<Int, Int>? {
  return vimPluginApi.getVisualMarks(editor, caretId)
}

fun Read.getChangeMarks(caretId: CaretId): Pair<Int, Int>? {
  return vimPluginApi.getChangeMarks(editor, caretId)
}

fun Read.getCaretLine(caretId: CaretId): Int? {
  return vimPluginApi.getCaretLine(editor, caretId)
}

fun Read.getLineStartOffset(line: Int): Int {
  return vimPluginApi.getLineStartOffset(editor, line)
}

fun Read.getLineEndOffset(line: Int, allowEnd: Boolean): Int {
  return vimPluginApi.getLineEndOffset(editor, line, allowEnd)
}

fun Read.getAllCaretsData(): List<CaretData> {
  return vimPluginApi.getAllCaretsData(editor)
}

fun Read.getAllCaretsDataSortedByOffset(): List<CaretData> {
  return vimPluginApi.getAllCaretsDataSortedByOffset(editor)
}

fun Read.getAllCaretIds(): List<CaretId> {
  return vimPluginApi.getAllCaretIds(editor)
}

fun Read.getAllCaretIdsSortedByOffset(): List<CaretId> {
  return vimPluginApi.getALlCaretIdsSortedByOffset(editor)
}

fun Read.getCaretInfo(caretId: CaretId): CaretInfo? {
  return vimPluginApi.getCaretInfo(editor, caretId)
}