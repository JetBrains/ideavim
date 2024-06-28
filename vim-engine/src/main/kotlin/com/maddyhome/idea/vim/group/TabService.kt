/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ExecutionContext

interface TabService {

  fun removeTabAt(indexToDelete: Int, indexToSelect: Int, context: ExecutionContext)
  fun getTabCount(context: ExecutionContext): Int
  fun getCurrentTabIndex(context: ExecutionContext): Int
  fun moveCurrentTabToIndex(index: Int, context: ExecutionContext)
  fun closeAllExceptCurrentTab(context: ExecutionContext)
}
