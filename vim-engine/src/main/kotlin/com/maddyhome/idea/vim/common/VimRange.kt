/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.LineDeleteShift

sealed class OperatedRange {
  class Lines(
    val text: CharSequence,
    val lineAbove: Int,
    val linesOperated: Int,
    val shiftType: LineDeleteShift,
  ) : OperatedRange()

  class Characters(val text: CharSequence, val leftOffset: Int, val rightOffset: Int) : OperatedRange()
  class Block : OperatedRange() {
    init {
      TODO()
    }
  }
}
