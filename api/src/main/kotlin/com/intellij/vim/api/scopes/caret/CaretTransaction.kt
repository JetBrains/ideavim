/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.caret

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.Range
import com.intellij.vim.api.RegisterData
import com.intellij.vim.api.RegisterType

interface CaretTransaction {
  val caretId: CaretId
  val caretInfo: CaretInfo

  fun getCurrentRegisterName(): Char
  fun getRegisterData(register: Char): RegisterData?
  fun getRegisterContent(register: Char): String?
  fun getRegisterType(register: Char): RegisterType?
  fun getVisualSelectionMarks(): Range?
  fun getChangeMarks(): Range?
  fun getCaretLine(): Int
  fun updateCaret(newInfo: CaretInfo)

  // todo: this is temporary - remove
  fun getLineStartOffset(line: Int): Int
  fun getLineEndOffset(line: Int, allowEnd: Boolean): Int
}