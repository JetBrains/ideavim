/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.VimEditor

public sealed class OperatedRange {
  public class Lines(
    public val text: CharSequence,
    public val lineAbove: Int,
    public val linesOperated: Int,
    public val shiftType: LineDeleteShift,
  ) : OperatedRange()

  public class Characters(public val text: CharSequence, public val leftOffset: Int, public val rightOffset: Int) : OperatedRange()
  public class Block : OperatedRange() {
    init {
      TODO()
    }
  }
}
