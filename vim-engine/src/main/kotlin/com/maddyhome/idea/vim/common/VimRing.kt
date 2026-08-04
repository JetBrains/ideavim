/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

/**
 * A bounded, ordered collection with most-recently-added last, plus a cursor for walking backwards
 * and forwards through it.
 *
 * @param maxSize Read on every add, so that changing the backing option takes effect immediately.
 * @param keyOf   Identity used for deduplication. Defaults to the item itself, but callers whose
 *                items carry incidental state that must not affect identity should narrow it. For
 *                example, two yanks of the same text produce `VimCopiedText` values that hold
 *                different IDE transferable data and therefore do not compare equal.
 */
class VimRing<T>(
  private val maxSize: () -> Int,
  private val keyOf: (T) -> Any? = { it },
) {
  private val entries = mutableListOf<T>()
  private var current: T? = null

  /**
   * The entry the cursor currently points at, or null when the cursor is past the newest entry.
   */
  val currentEntry: T?
    get() = current

  /**
   * The most recently added entry, or null when the ring is empty.
   */
  val mostRecentEntry: T?
    get() = entries.lastOrNull()

  /**
   * All entries, oldest first.
   */
  fun getEntries(): List<T> = entries

  /**
   * Adds an item as the newest entry.
   *
   * An existing entry with the same key is removed first, so the item ends up newest instead of
   * appearing twice. Entries beyond [maxSize] are dropped from the oldest end, and the cursor is
   * reset to "past the newest entry".
   */
  fun add(item: T) {
    val key = keyOf(item)
    val existing = entries.indexOfFirst { keyOf(it) == key }
    if (existing != -1) {
      entries.removeAt(existing)
    }

    entries.add(item)
    resetCurrentEntry()

    // A single add can only overflow by one, but the limit is read live and may have shrunk since
    // the last add, so trim until we're back within it.
    while (entries.size > maxSize() && entries.isNotEmpty()) {
      entries.removeAt(0)
    }
  }

  /**
   * Removes the first entry matching [predicate] and resets the cursor.
   *
   * @return true if an entry was removed
   */
  fun remove(predicate: (T) -> Boolean): Boolean {
    val index = entries.indexOfFirst(predicate)
    if (index == -1) return false

    entries.removeAt(index)
    resetCurrentEntry()
    return true
  }

  /**
   * Moves the cursor one step towards the newest entry, skipping entries rejected by [predicate].
   *
   * Stepping past the newest entry leaves the cursor unset and returns null, which is how the
   * command line returns to the text the user was originally typing.
   */
  fun selectNewer(predicate: (T) -> Boolean = { true }): T? {
    // We're already past the newest entry, so there's nothing newer
    if (current == null) return null

    var index = entries.indexOf(current) + 1
    while (index != entries.size && !predicate(entries[index])) {
      index++
    }

    if (index == entries.size) {
      current = null
      return null
    }

    current = entries[index]
    return current
  }

  /**
   * Moves the cursor one step towards the oldest entry, skipping entries rejected by [predicate].
   *
   * Returns null and leaves the cursor where it was once there is nothing older to move to.
   */
  fun selectOlder(predicate: (T) -> Boolean = { true }): T? {
    var index = if (current == null) entries.size - 1 else entries.indexOf(current) - 1
    while (index >= 0 && !predicate(entries[index])) {
      index--
    }

    if (index < 0) return null

    current = entries[index]
    return current
  }

  /**
   * Removes every entry and resets the cursor.
   */
  fun clear() {
    entries.clear()
    resetCurrentEntry()
  }

  private fun resetCurrentEntry() {
    // Reset the current entry to null, indicating we're past the end of the ring
    current = null
  }
}
