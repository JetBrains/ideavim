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
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.scopes.caret.CaretRead

@VimPluginDsl
interface Read {
  val textLength: Long
  val text: CharSequence
  val lineCount: Int

  fun <T> forEachCaret(block: CaretRead.() -> T): List<T>
  fun with(caretId: CaretId, block: CaretRead.() -> Unit)

  fun getLineStartOffset(line: Int): Int
  fun getLineEndOffset(line: Int, allowEnd: Boolean): Int

  fun getLine(offset: Int): Line

  val caretData: List<CaretData>
  val caretIds: List<CaretId>

  /**
   * Gets a global mark by its character key.
   *
   * @param char The character key of the mark (A-Z)
   * @return The mark, or null if the mark doesn't exist
   */
  fun getGlobalMark(char: Char): Mark?

  /**
   * Gets all global marks.
   *
   * @return A set of all global marks
   */
  fun getAllGlobalMarks(): Set<Mark>
}