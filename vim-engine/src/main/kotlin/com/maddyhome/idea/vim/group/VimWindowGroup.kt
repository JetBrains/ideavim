/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ExecutionContext

interface VimWindowGroup {
  fun selectWindowInRow(context: ExecutionContext, relativePosition: Int, vertical: Boolean)
  fun selectNextWindow(context: ExecutionContext)
  fun selectWindow(context: ExecutionContext, index: Int)
  fun selectPreviousWindow(context: ExecutionContext)
  fun closeAllExceptCurrent(context: ExecutionContext)
  fun splitWindowVertical(context: ExecutionContext, filename: String)
  fun splitWindowHorizontal(context: ExecutionContext, filename: String)
  fun closeCurrentWindow(context: ExecutionContext)
  fun closeAll(context: ExecutionContext)
}
