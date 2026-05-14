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

internal class HistoryBlock {
  private val entries = mutableListOf<HistoryEntry>()

  private var counter = 0

  fun addEntry(text: String) {
    if (text.isEmpty()) return

    // If we have a last entry, it's no longer the current one
    if (entries.isNotEmpty()) {
      val last = entries.removeLast()
      entries.add(HistoryEntry(last.number, last.entry, current = false))
    }

    // If this entry already exists, remove it so we can add it as the newest entry
    for (i in entries.indices) {
      val entry = entries[i]
      if (text == entry.entry) {
        entries.removeAt(i)
        break
      }
    }

    // Add the new entry as the current one
    entries.add(HistoryEntry(++counter, text, current = true))

    // If we're over the maximum number of entries, remove the oldest one
    if (entries.size > maxLength()) {
      entries.removeAt(0)
    }
  }

  fun removeEntryByNumber(number: Int): Boolean {
    val index = entries.indexOfFirst { it.number == number }
    if (index != -1) {
      val entry = entries.removeAt(index)
      if (entry.current) {
        entries.removeLastOrNull()?.let { entries.add(it.copy(current = true)) }
      }
      return true
    }
    return false
  }

  fun getEntries(): List<HistoryEntry> {
    return entries
  }

  companion object {
    private fun maxLength() = injector.globalOptions().history
  }
}
