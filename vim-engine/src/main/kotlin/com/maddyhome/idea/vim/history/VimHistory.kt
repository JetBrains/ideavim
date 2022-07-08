package com.maddyhome.idea.vim.history

interface VimHistory {
  fun addEntry(key: String, text: String)
  fun getEntries(key: String, first: Int, last: Int): List<HistoryEntry>
}
