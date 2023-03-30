/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector

public class HistoryBlock {
  private val entries: MutableList<HistoryEntry> = ArrayList()

  private var counter = 0

  public fun addEntry(text: String) {
    for (i in entries.indices) {
      val entry = entries[i]
      if (text == entry.entry) {
        entries.removeAt(i)
        break
      }
    }
    entries.add(HistoryEntry(++counter, text))
    if (entries.size > maxLength()) {
      entries.removeAt(0)
    }
  }

  public fun getEntries(): List<HistoryEntry> {
    return entries
  }

  public companion object {
    private fun maxLength() = injector.globalOptions().history
  }
}
