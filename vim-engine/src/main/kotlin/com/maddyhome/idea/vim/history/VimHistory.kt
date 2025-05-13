/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

interface VimHistory {
  fun addEntry(type: Type, text: String)
  fun getEntries(type: Type, first: Int, last: Int): List<HistoryEntry>

  sealed class Type() {
    data object Search : Type()
    data object Command : Type()
    data object Expression : Type()
    data object Input : Type()
    data class Custom(val id: String) : Type()

    companion object {
      fun getTypeByLabel(label: String): Type {
        return when (label) {
          ":" -> Command
          "/", "?" -> Search
          "=" -> Expression
          else -> Custom(label)
        }
      }
    }
  }
}
