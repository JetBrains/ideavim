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

public class LineRange(startLine: Int, endLine: Int) {

  @JvmField
  public val startLine: Int

  @JvmField
  public val endLine: Int

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

public fun LineRange.toTextRange(editor: VimEditor): TextRange {
  val start = editor.getLineStartOffset(startLine)
  val end = editor.getLineEndOffset(endLine, true) + 1
  return TextRange(start, min(end, editor.fileSize().toInt()))
}
