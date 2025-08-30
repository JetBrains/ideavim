/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger

open class VimHistoryBase : VimHistory {
  protected val histories: MutableMap<VimHistory.Type, HistoryBlock> = mutableMapOf()

  override fun addEntry(type: VimHistory.Type, text: String) {
    val block = getEntriesBlockByType(type)
    block.addEntry(text)
  }

  override fun getEntries(type: VimHistory.Type, first: Int, last: Int): List<HistoryEntry> {
    var myFirst = first
    var myLast = last
    val block = getEntriesBlockByType(type)

    val entries = block.getEntries()
    if (myFirst == 0 && myLast == 0) {
      myLast = Integer.MAX_VALUE
    }
    if (myFirst < 0) {
      myFirst = if (-myFirst > entries.size) {
        Integer.MAX_VALUE
      } else {
        val entry = entries[entries.size + myFirst]
        entry.number
      }
    }
    if (myLast < 0) {
      myLast = if (-myLast > entries.size) {
        Integer.MIN_VALUE
      } else {
        val entry = entries[entries.size + myLast]
        entry.number
      }
    } else if (myLast == 0) {
      myLast = myFirst
    }

    logger.debug { "first=$myFirst\nlast=$myLast" }

    val res = mutableListOf<HistoryEntry>()
    for (entry in entries) {
      if (entry.number in myFirst..myLast) {
        res.add(entry)
      }
    }

    return res
  }

  override fun resetHistory() {
    histories.clear()
  }

  private fun getEntriesBlockByType(type: VimHistory.Type): HistoryBlock {
    return histories.getOrPut(type) { HistoryBlock() }
  }

  companion object {
    val logger: VimLogger = vimLogger<VimHistoryBase>()
  }
}
