/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.TabScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.injector

class TabScopeImpl(
  private val vimContext: ExecutionContext,
) : TabScope {
  override val count: Int
    get() = injector.tabService.getTabCount(vimContext)

  override val currentIndex: Int?
    get() = injector.tabService.getCurrentTabIndex(vimContext)

  override suspend fun removeAt(indexToDelete: Int, indexToSelect: Int) {
    injector.tabService.removeTabAt(indexToDelete, indexToSelect, vimContext)
  }

  override suspend fun moveCurrentToIndex(index: Int) {
    injector.tabService.moveCurrentTabToIndex(index, vimContext)
  }

  override suspend fun closeAllExceptCurrent() {
    injector.tabService.closeAllExceptCurrentTab(vimContext)
  }
}
