/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

public interface VimHistory {
  public fun addEntry(key: String, text: String)
  public fun getEntries(key: String, first: Int, last: Int): List<HistoryEntry>
}
