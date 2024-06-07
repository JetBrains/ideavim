/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import kotlin.properties.Delegates

internal data class InsertSequence(val startOffset: Int, val startNanoTime: Long) {
  var endOffset by Delegates.notNull<Int>()
  var endNanoTime by Delegates.notNull<Long>()

  fun contains(nanoTime: Long): Boolean {
    return nanoTime in startNanoTime..endNanoTime
  }
}