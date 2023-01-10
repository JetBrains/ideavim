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

  class Change(val oldFragment: String, val newFragment: String, val offset: Int)
}
