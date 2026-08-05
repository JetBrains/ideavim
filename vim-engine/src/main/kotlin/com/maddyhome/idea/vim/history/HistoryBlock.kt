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
import com.maddyhome.idea.vim.common.VimRing

internal class HistoryBlock {
  // Entries are deduplicated by their text: the number is assigned per add, so including it in the
  // key would make every entry unique and defeat the deduplication.
  private val ring = VimRing<HistoryEntry>(
    maxSize = { injector.globalOptions().history },
    keyOf = { it.entry },
  )
  private var counter = 0

  val currentEntry: HistoryEntry?
    get() = ring.currentEntry

  val mostRecentEntry: HistoryEntry?
    get() = ring.mostRecentEntry

  fun addEntry(text: String) {
    if (text.isEmpty()) return

    ring.add(HistoryEntry(++counter, text))
  }

  fun removeEntryByNumber(number: Int): Boolean = ring.remove { it.number == number }

  fun getEntries(): List<HistoryEntry> = ring.getEntries()

  fun selectNewerEntry(filter: String?): HistoryEntry? = ring.selectNewer(matching(filter))

  fun selectOlderEntry(filter: String?): HistoryEntry? = ring.selectOlder(matching(filter))

  private fun matching(filter: String?): (HistoryEntry) -> Boolean =
    if (filter == null) {
      { true }
    } else {
      { it.entry.startsWith(filter) }
    }
}
