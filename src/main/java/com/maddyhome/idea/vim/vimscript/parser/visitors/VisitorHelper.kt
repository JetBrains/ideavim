/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.common.TextRange
import org.antlr.v4.runtime.ParserRuleContext

public fun ParserRuleContext.getTextRange(): TextRange {
  val startOffset = this.start.startIndex
  val endOffset = this.stop.stopIndex + 1
  return TextRange(startOffset, endOffset)
}
