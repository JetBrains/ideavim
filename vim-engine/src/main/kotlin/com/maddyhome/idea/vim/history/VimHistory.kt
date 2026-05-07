/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

import org.jetbrains.annotations.TestOnly

interface VimHistory {
  /**
   * Get a list of history entries for the given history type, within the specified range
   *
   * The values of [first] and [last] can be either positive or negative. When the value is negative, it is an index
   * relative to the end of the history list. When positive, it is the item's absolute index. Remember that duplicate
   * items are removed when a new item is added, leaving an empty "slot" in the list. This means an absolute index might
   * not contain an item. See `:help history-indexing` for more details.
   *
   * The function will return any history items whose absolute index is within the range specified by [first] and
   * [last]. If [last] is `0`, all entries after [first] are returned. If both values are `0`, all entries are returned.
   */
  fun getEntries(type: Type, first: Int, last: Int): List<HistoryEntry>

  /**
   * Add an item to the end of the list for the given history type
   */
  fun addEntry(type: Type, text: String)

  /**
   * Removes an item from the history, either by number or relative position
   *
   * If [item] is negative, it is relative to the end of the history list. If positive, it is the number of the item in
   * the list of entries (not an index!). See `:help history-indexing` for more details.
   */
  fun removeEntry(type: Type, item: Int): Boolean

  /**
   * Remove any entries from the history that match the given regex pattern
   */
  fun removeEntries(type: Type, pattern: String): Boolean

  /**
   * Clear the entire history for the given history type
   */
  fun clearHistory(type: Type)

  @TestOnly
  fun resetHistory()

  sealed class Type {
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

      fun getTypeByString(value: String): Type? {
        if ("cmd".startsWith(value)) return Command
        if ("search".startsWith(value)) return Search
        if ("expression".startsWith(value)) return Expression
        if ("input".startsWith(value)) return Input
        return when (value) {
          ":" -> Command
          "/" -> Search
          "=" -> Expression
          "@" -> Input
          else -> null
        }
      }
    }
  }
}
