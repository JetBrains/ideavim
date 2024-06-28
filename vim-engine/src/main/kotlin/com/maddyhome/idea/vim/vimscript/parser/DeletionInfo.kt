/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser

/**
 * It's a helper class to store information about all the deleted substrings during parsing
 */
class DeletionInfo {
  // First number is deletion offset, the second one - number of deleted chars
  private val deletions = mutableListOf<Pair<Int, Int>>()

  fun registerDeletion(startOffset: Int, length: Int) {
    deletions.add(Pair(startOffset, length))
  }

  fun restoreOriginalOffset(finalOffset: Int): Int {
    var result = finalOffset
    for ((i, length) in deletions.reversed()) {
      if (result >= i) {
        result += length
      }
    }
    return result
  }

  fun reset() {
    deletions.clear()
  }
}
