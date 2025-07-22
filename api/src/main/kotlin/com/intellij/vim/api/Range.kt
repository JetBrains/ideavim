/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

sealed interface Range {
  data class Simple(val start: Int, val end: Int): Range

  data class Block(val ranges: Array<Simple>): Range {
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