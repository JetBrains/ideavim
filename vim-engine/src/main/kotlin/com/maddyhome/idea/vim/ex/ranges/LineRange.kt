/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex.ranges

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.common.TextRange
import kotlin.math.min

/**
 * Represents a line range
 *
 * A line range is specified by the user as 1-based, but can include the value `0`. E.g., the copy and move commands
 * in the form `:[range]copy {address}` treat an address with a line of 0 as meaning copy/move the range to _above_ the
 * first line (all other values are _below_ the address).
 *
 * The [startLine] and [endLine] fields are 0-based, matching the rest of IdeaVim. These are coerced to a minimum value
 * of `0`, so the range `0,$` becomes a synonym for `1,$`.
 */
class LineRange(startLine: Int, endLine: Int) {
  @JvmField
  val startLine: Int

  @JvmField
  val endLine: Int

  init {
    if (endLine >= startLine) {
      this.startLine = startLine.coerceAtLeast(0)
      this.endLine = endLine.coerceAtLeast(0)
    } else {
      this.startLine = endLine.coerceAtLeast(0)
      this.endLine = startLine.coerceAtLeast(0)
    }
  }

  val startLine1: Int = startLine + 1
  val endLine1: Int = endLine + 1
  val size: Int = endLine - startLine + 1
}

fun LineRange.toTextRange(editor: VimEditor): TextRange {
  val start = editor.getLineStartOffset(startLine.coerceAtLeast(0))
  val end = editor.getLineEndOffset(endLine, true) + 1
  return TextRange(start, min(end, editor.fileSize().toInt()))
}
