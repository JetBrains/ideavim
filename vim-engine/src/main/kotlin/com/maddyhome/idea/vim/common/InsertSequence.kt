/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import kotlin.properties.Delegates

data class InsertSequence(val startOffset: Int, val startNanoTime: Long) {
  var endOffset: Int by Delegates.notNull()
  var endNanoTime: Long by Delegates.notNull()

  fun contains(nanoTime: Long): Boolean {
    return nanoTime in startNanoTime..endNanoTime
  }
}