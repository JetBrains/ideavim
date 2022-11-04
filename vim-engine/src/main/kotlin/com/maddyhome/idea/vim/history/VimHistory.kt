/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

interface VimHistory {
  fun addEntry(key: String, text: String)
  fun getEntries(key: String, first: Int, last: Int): List<HistoryEntry>
}
