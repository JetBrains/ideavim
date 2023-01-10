/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex.ranges

class LineRange(startLine: Int, endLine: Int) {

  @JvmField
  val startLine: Int

  @JvmField
  val endLine: Int

  init {
    if (endLine >= startLine) {
      this.startLine = startLine
      this.endLine = endLine
    } else {
      this.startLine = endLine
      this.endLine = startLine
    }
  }
}
