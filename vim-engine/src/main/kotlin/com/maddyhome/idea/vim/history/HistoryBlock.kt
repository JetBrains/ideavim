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
  private var current: HistoryEntry? = null

  /**
   * Returns the current history entry if available, or null otherwise
   */
  val currentEntry: HistoryEntry?
    get() = current

  /**
   * Returns the most recent entry in the history, the last saved value, or null
   */
  val mostRecentEntry: HistoryEntry?
    get() = entries.lastOrNull()

  fun addEntry(text: String) {
    if (text.isEmpty()) return

    // If this entry already exists, remove it so we can add it as the newest entry
    for (i in entries.indices) {
      if (text == entries[i].entry) {
        entries.removeAt(i)
        break
      }
    }

    entries.add(HistoryEntry(++counter, text))
    resetCurrentEntry()

    // If we're over the maximum number of entries, remove the oldest one
    if (entries.size > maxLength()) {
      entries.removeAt(0)
    }
  }

  fun removeEntryByNumber(number: Int): Boolean {
    val index = entries.indexOfFirst { it.number == number }
    if (index != -1) {
      entries.removeAt(index)
      resetCurrentEntry()
      return true
    }
    return false
  }

  fun getEntries(): List<HistoryEntry> {
    return entries
  }

  fun selectNewerEntry(filter: String?): HistoryEntry? {
    if (current == null) {
      // We're at the end of history, so there's no newer entry
      return null
    }

    var index = entries.indexOf(current) + 1
    while (filter != null && index != entries.size && !entries[index].entry.startsWith(filter)) {
      index++
    }

    if (index == entries.size) {
      current = null
      return null
    }

    current = entries[index]
    return current
  }

  fun selectOlderEntry(filter: String?): HistoryEntry? {
    var index = if (current == null) (entries.size - 1) else (entries.indexOf(current) - 1)
    while (filter != null && index >= 0 && !entries[index].entry.startsWith(filter)) {
      index--
    }

    if (index < 0) {
      return null
    }

    current = entries[index]
    return current
  }

  private fun resetCurrentEntry() {
    // Reset the current entry to null, indicating we're past the end of the history
    current = null
  }

  companion object {
    private fun maxLength() = injector.globalOptions().history
  }
}
