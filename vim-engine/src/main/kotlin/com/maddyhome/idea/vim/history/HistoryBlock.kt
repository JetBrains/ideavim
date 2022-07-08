package com.maddyhome.idea.vim.history

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

class HistoryBlock {
  private val entries: MutableList<HistoryEntry> = ArrayList()

  private var counter = 0

  fun addEntry(text: String) {
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

  fun getEntries(): List<HistoryEntry> {
    return entries
  }

  companion object {
    private fun maxLength(): Int {
      return (
        injector.optionService
          .getOptionValue(
            OptionScope.GLOBAL, OptionConstants.historyName,
            OptionConstants.historyName
          ) as VimInt
        ).value
    }
  }
}
