/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo

interface Transaction : Read

fun Transaction.deleteText(startOffset: Int, endOffset: Int) {
  vimPluginApi.deleteText(editor, startOffset, endOffset)
}

fun Transaction.replaceText(
  caretId: CaretId,
  startOffset: Int,
  endOffset: Int,
  text: String,
) {
  vimPluginApi.replaceText(editor, caretId, startOffset, endOffset, text)
}

fun Transaction.replaceTextBlockwise(
  caretId: CaretId,
  startOffset: Int,
  endOffset: Int,
  text: List<String>
) {
  vimPluginApi.replaceTextBlockwise(editor, caretId, startOffset, endOffset, text)
}

fun Transaction.updateCaret(caretId: CaretId, info: CaretInfo) {
  vimPluginApi.updateCaret(editor, caretId, info)
}
