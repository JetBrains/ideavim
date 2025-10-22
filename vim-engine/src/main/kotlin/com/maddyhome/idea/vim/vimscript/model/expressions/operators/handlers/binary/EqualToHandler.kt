/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

internal class EqualToHandler(ignoreCase: Boolean? = null) : ComparisonOperatorHandler(ignoreCase) {
  override fun compare(left: Double, right: Double) = left == right
  override fun compare(left: Int, right: Int) = left == right
  override fun compare(left: String, right: String, ignoreCase: Boolean) = left.compareTo(right, ignoreCase) == 0

  override fun compare(left: VimList, right: VimList, ignoreCase: Boolean, depth: Int): Boolean {
    // If the recursive structure is deep enough, treat it as equal.
    // The value is fairly arbitrary but based on Vim's behaviour. Vim will also reduce the limit for every comparison
    // once we're deep enough, so the tail of a list will both reduce the limit and short-circuit any further
    // comparisons, including potentially expensive nested comparisons.
    // So it should be possible to create a data structure that is 1001 levels deep (the first comparison is level 0)
    // but has different values in the tail of the list, and Vim would still treat it as equal.
    if (depth > 1000) return true

    if (left === right) return true
    if (left.values.size != right.values.size) return false
    if (left.values.isEmpty()) return true
    return left.values.zip(right.values).all { (first, second) ->
      first::class == second::class && doCompare(first, second, ignoreCase, depth + 1)
    }
  }

  override fun compare(left: VimDictionary, right: VimDictionary, ignoreCase: Boolean, depth: Int): Boolean {
    if (depth > 1000) return true

    if (left === right) return true
    if (left.dictionary.size != right.dictionary.size) return false
    if (left.dictionary.isEmpty()) return true
    return left.dictionary.all { (key, value) ->
      right.dictionary[key]?.let {
        value::class == it::class && doCompare(value, it, ignoreCase, depth + 1)
      } ?: false
    }
  }

  // TODO: Implement for Funcref, Blob
}
