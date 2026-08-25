/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.tag.TagStackEntry

abstract class VimTagServiceBase : VimTagService {
  protected val scopeToEntries: MutableMap<String, MutableList<TagStackEntry>> = mutableMapOf()
  protected val scopeToCurrentIndex: MutableMap<String, Int> = mutableMapOf()
  private val scopesWithOwnStack = mutableSetOf<String>()

  override fun getEntries(scopeId: String): List<TagStackEntry> {
    return scopeToEntries[scopeId] ?: emptyList()
  }

  override fun getCurrentIndex(scopeId: String): Int {
    return scopeToCurrentIndex[scopeId] ?: 0
  }

  override fun push(scopeId: String, entry: TagStackEntry) {
    scopesWithOwnStack.add(scopeId)
    val entries = scopeToEntries.getOrPut(scopeId) { mutableListOf() }

    // Entries the user walked back past are unreachable once a new tag jump is made, as in a browser history
    val currentIndex = getCurrentIndex(scopeId)
    if (currentIndex < entries.size) {
      entries.subList(currentIndex, entries.size).clear()
    }

    entries.add(entry)
    if (entries.size > TAG_STACK_SIZE) {
      entries.removeFirst()
    }
    scopeToCurrentIndex[scopeId] = entries.size
  }

  override fun pop(scopeId: String, count: Int): TagStackEntry? {
    val entries = scopeToEntries[scopeId] ?: return null
    if (entries.isEmpty()) return null

    val index = getCurrentIndex(scopeId) - count
    if (index < 0) return null

    scopeToCurrentIndex[scopeId] = index
    return entries[index]
  }

  override fun moveDown(scopeId: String, count: Int): TagStackEntry? {
    val entries = scopeToEntries[scopeId] ?: return null
    if (entries.isEmpty()) return null

    val index = getCurrentIndex(scopeId) + count
    if (index > entries.size) return null

    scopeToCurrentIndex[scopeId] = index
    return entries[index - 1]
  }

  override fun clear(scopeId: String) {
    // An emptied stack is still the scope's own, so it must not inherit another one later
    scopesWithOwnStack.add(scopeId)
    scopeToEntries.remove(scopeId)
    scopeToCurrentIndex.remove(scopeId)
  }

  override fun copyTagStack(fromId: String, toId: String) {
    val entries = scopeToEntries[fromId] ?: return
    scopeToEntries[toId] = entries.toMutableList()
    scopeToCurrentIndex[toId] = scopeToCurrentIndex[fromId] ?: 0
  }

  override fun inheritTagStack(fromId: String, toId: String) {
    if (toId in scopesWithOwnStack || scopeToEntries.containsKey(toId)) return
    // An inherited empty stack is the window's own, as in Vim - not an absence to be filled in later
    scopesWithOwnStack.add(toId)
    scopeToEntries[toId] = scopeToEntries[fromId]?.toMutableList() ?: mutableListOf()
    scopeToCurrentIndex[toId] = scopeToCurrentIndex[fromId] ?: 0
  }

  override fun resetTagStacks() {
    scopeToEntries.clear()
    scopeToCurrentIndex.clear()
    scopesWithOwnStack.clear()
  }

  companion object {
    /** Vim's TAGSTACKSIZE */
    const val TAG_STACK_SIZE: Int = 20
  }
}
