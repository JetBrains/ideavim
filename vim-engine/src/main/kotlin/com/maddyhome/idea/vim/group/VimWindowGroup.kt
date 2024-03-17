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

public interface VimWindowGroup {
  public fun selectWindowInRow(caret: VimCaret, context: ExecutionContext, relativePosition: Int, vertical: Boolean)
  public fun selectNextWindow(context: ExecutionContext)
  public fun selectWindow(context: ExecutionContext, index: Int)
  public fun selectPreviousWindow(context: ExecutionContext)
  public fun closeAllExceptCurrent(context: ExecutionContext)
  public fun splitWindowVertical(context: ExecutionContext, filename: String)
  public fun splitWindowHorizontal(context: ExecutionContext, filename: String)
  public fun closeCurrentWindow(context: ExecutionContext)
  public fun closeAll(context: ExecutionContext)
}
