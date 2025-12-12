/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.models

/**
 * Represents a range of text in the editor.
 * Can be either a simple linear range or a block (rectangular) range.
 *
 * Ranges are **normalized**: [start] is always less than or equal to [end],
 * regardless of the selection direction. The [end] offset is exclusive.
 *
 * @see <a href="https://youtrack.jetbrains.com/issue/VIM-XXXX">ADR: Normalized Selection Ranges</a>
 */
sealed interface Range {
  /**
   * Represents a simple linear range of text from start to end offset.
   *
   * @property start The starting offset of the range (inclusive).
   * @property end The ending offset of the range (exclusive).
   */
  data class Simple(val start: Int, val end: Int) : Range

  /**
   * Represents a block (rectangular) selection defined by two corner offsets.
   * The block spans from [start] to [end], where the actual rectangular region
   * is determined by the line/column positions of these offsets.
   *
   * @property start The starting offset of the block selection.
   * @property end The ending offset of the block selection.
   */
  data class Block(val start: Int, val end: Int) : Range
}
