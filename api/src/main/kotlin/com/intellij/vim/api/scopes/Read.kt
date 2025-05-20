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

interface Read : VimPluginScope

fun Read.getCurrentRegisterName(caretId: CaretId): Char {
  return vimPluginApi.getCurrentRegisterName(this, caretId)
}

fun Read.getRegisterContent(caretId: CaretId, register: Char): String? {
  return vimPluginApi.getRegisterContent(this, caretId, register)
}

fun Read.getRegisterType(caretId: CaretId, register: Char): RegisterType? {
  return vimPluginApi.getRegisterType(this, caretId, register)
}

fun Read.getVisualSelectionMarks(caretId: CaretId): Pair<Int, Int>? {
  return vimPluginApi.getVisualMarks(this, caretId)
}

fun Read.getChangeMarks(caretId: CaretId): Pair<Int, Int>? {
  return vimPluginApi.getChangeMarks(this, caretId)
}

fun Read.getCaretLine(caretId: CaretId): Int? {
  return vimPluginApi.getCaretLine(this, caretId)
}

fun Read.getLineStartOffset(line: Int): Int {
  return vimPluginApi.getLineStartOffset(this, line)
}

fun Read.getLineEndOffset(line: Int, allowEnd: Boolean): Int {
  return vimPluginApi.getLineEndOffset(this, line, allowEnd)
}

fun Read.getAllCaretsData(): List<CaretData> {
  return vimPluginApi.getAllCaretsData(this)
}

fun Read.getAllCaretsDataSortedByOffset(): List<CaretData> {
  return vimPluginApi.getAllCaretsDataSortedByOffset(this)
}

fun Read.getAllCaretIds(): List<CaretId> {
  return vimPluginApi.getAllCaretIds(this)
}

fun Read.getAllCaretIdsSortedByOffset(): List<CaretId> {
  return vimPluginApi.getALlCaretIdsSortedByOffset(this)
}

fun Read.getCaretInfo(caretId: CaretId): CaretInfo? {
  return vimPluginApi.getCaretInfo(this, caretId)
}