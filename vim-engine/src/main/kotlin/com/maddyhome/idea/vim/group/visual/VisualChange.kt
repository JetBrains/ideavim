/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.state.mode.SelectionType

data class VisualChange(val lines: Int, val columns: Int, val type: SelectionType) {
  companion object {
    fun default(selectionType: SelectionType): VisualChange =
      when (selectionType) {
        SelectionType.LINE_WISE, SelectionType.CHARACTER_WISE -> VisualChange(1, 1, selectionType)
        SelectionType.BLOCK_WISE -> VisualChange(0, 1, selectionType)
      }
  }
}
