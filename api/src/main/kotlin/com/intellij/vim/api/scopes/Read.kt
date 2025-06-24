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
import com.intellij.vim.api.Jump
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
   * All global marks.
   */
  val globalMarks: Set<Mark>

  /**
   * Gets a jump from the jump list.
   *
   * @param count The number of jumps to go back (negative) or forward (positive) from the current position in the jump list.
   * @return The jump, or null if there is no jump at the specified position
   */
  fun getJump(count: Int = 0): Jump?

  /**
   * Gets all jumps in the jump list.
   *
   * @return A list of all jumps
   */
  val jumps: List<Jump>

  /**
   * Index of the current position in the jump list.
   *
   * This is used to determine which jump will be used when navigating with Ctrl-O and Ctrl-I.
   */
  val currentJumpIndex: Int
}