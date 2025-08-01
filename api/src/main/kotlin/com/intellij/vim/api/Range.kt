/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

/**
 * Represents a range of text in the editor.
 * Can be either a simple linear range or a block (rectangular) range.
 */
sealed interface Range {
  /**
   * Represents a simple linear range of text from start to end offset.
   *
   * @property start The starting offset of the range.
   * @property end The ending offset of the range (exclusive).
   */
  data class Simple(val start: Int, val end: Int) : Range

  /**
   * Represents a block (rectangular) selection consisting of multiple simple ranges.
   * Each simple range typically represents a line segment in the block selection.
   *
   * @property ranges An array of simple ranges that make up the block selection.
   */
  data class Block(val ranges: Array<Simple>) : Range {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Block
      return ranges.contentEquals(other.ranges)
    }

    override fun hashCode(): Int {
      return ranges.contentHashCode()
    }
  }
}
