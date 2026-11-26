/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

interface ChangesListener {
  fun documentChanged(change: Change)

  /**
   * @param virtualSpaceFill The number of characters at the start of [newFragment] that only fill the virtual space
   *                         between the end of the line and a caret placed after it
   */
  class Change(val oldFragment: String, val newFragment: String, val offset: Int, val virtualSpaceFill: Int = 0)
}
