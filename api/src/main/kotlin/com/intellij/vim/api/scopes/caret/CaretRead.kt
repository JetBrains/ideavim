/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.caret

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Line
import com.intellij.vim.api.Range
import com.intellij.vim.api.TextType

interface CaretRead {
  val caretId: CaretId

  val offset: Int
  val selection: Array<Range>
  val line: Line

  val lastSelectedReg: Char

  val visualSelectionMarks: Array<Range>?
  val changeMarks: Range?

  fun getReg(register: Char): String?
  fun getRegType(register: Char): TextType?
}