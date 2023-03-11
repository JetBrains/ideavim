/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret

interface VimWindowGroup {
  fun selectWindowInRow(caret: VimCaret, context: ExecutionContext, relativePosition: Int, vertical: Boolean)
  fun selectNextWindow(context: ExecutionContext)
  fun selectWindow(context: ExecutionContext, index: Int)
  fun selectPreviousWindow(context: ExecutionContext)
  fun closeAllExceptCurrent(context: ExecutionContext)
  fun splitWindowVertical(context: ExecutionContext, filename: String)
  fun splitWindowHorizontal(context: ExecutionContext, filename: String)
  fun closeCurrentWindow(context: ExecutionContext)
  fun closeAll(context: ExecutionContext)
}
