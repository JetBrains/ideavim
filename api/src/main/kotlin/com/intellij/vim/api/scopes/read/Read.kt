/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.read

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.RegisterType

interface Read {
  fun getCurrentRegisterName(caretId: CaretId): Char
  fun getRegisterContent(caretId: CaretId, register: Char): String?
  fun getRegisterType(caretId: CaretId, register: Char): RegisterType?
  fun getVisualSelectionMarks(caretId: CaretId): Pair<Int, Int>?
  fun getChangeMarks(caretId: CaretId): Pair<Int, Int>?
  fun getCaretLine(caretId: CaretId): Int?
  fun getLineStartOffset(line: Int): Int
  fun getLineEndOffset(line: Int, allowEnd: Boolean): Int
  fun getAllCaretsData(): List<CaretData>
  fun getAllCaretsDataSortedByOffset(): List<CaretData>
  fun getAllCaretIds(): List<CaretId>
  fun getAllCaretIdsSortedByOffset(): List<CaretId>
  fun getCaretInfo(caretId: CaretId): CaretInfo?
}