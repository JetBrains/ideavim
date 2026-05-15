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
   * Get the current history entry, if selected, or null otherwise
   *
   * Vim keeps track of the current entry while the user navigates through the history while editing the command line
   * (and search/input/expression entry). When the command line is first displayed, there is no stored current history
   * entry, only the current entry being edited. That means the current entry is initially `null`, and represents the
   * item *after* the most recent entry, past the end of the history list.
   *
   * When the user navigates to an older entry with `<S-Up>` or filtered older entry with `<Up>`, the current entry is
   * moved to the next oldest entry - starting with the last entry in the list, the most recently saved entry. When the
   * command line is completed or canceled, the new entry is saved and the current entry is reset.
   */
  fun getCurrentEntry(type: Type): HistoryEntry?

  /**
   * Get the most recent history entry
   *
   * This function returns the most recently saved history entry, the item at the end of the history list, if available.
   * It returns `null` if there are no saved entries.
   *
   * This is used to mark the current entry when displaying entries in the output of the `:history` command, when the
   * current entry has a default value of `null` (meaning past the last entry in the history).
   */
  fun getMostRecentEntry(type: Type): HistoryEntry?

  /**
   * Select the next older entry from the given type's history, if possible, updating the current entry
   *
   * This is typically used when navigating through the history using the `<Up>` or `<S-Up>` keys. By default, the
   * current entry starts as being the entry past the end of the history list, so pressing up will get the most recent
   * entry. Pressing up again would get the next older entry, which would be the second most recent entry, and so on.
   *
   * If the filter value is given, the next older entry must start with the value.
   *
   * If there are no matching entries, or the start of the history is reached, the current entry will not be updated and
   * `null` is returned.
   */
  fun selectOlderEntry(type: Type, filter: String?): HistoryEntry?

  /**
   * Select the next newer entry from the given type's history, if possible, updating the current entry
   *
   * This function is typically used when navigating through the history using the `<Down>` or `<S-Down>` keys. By
   * default, the current entry starts as being the entry past the beginning of the history list, and there are no newer
   * entries. If the user navigates to older entries, this function will return the next newer entry after the current
   * entry, updating the current entry as it does.
   *
   * If the filter value is given, the next newer entry must start with the value.
   *
   * If there are no matching entries, or the end of the history is reached, the current entry will not be updated and
   * `null` is returned.
   */
  fun selectNewerEntry(type: Type, filter: String?): HistoryEntry?

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
