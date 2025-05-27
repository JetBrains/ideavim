/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.transaction

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.scopes.read.Read

interface Transaction : Read {
  fun deleteText(startOffset: Int, endOffset: Int)
  fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  )
  fun replaceTextBlockwise(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>
  )
  fun updateCaret(caretId: CaretId, info: CaretInfo)
}