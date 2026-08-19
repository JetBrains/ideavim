/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump

abstract class VimJumpServiceBase : VimJumpService {
  protected val scopeToJumps: MutableMap<String, MutableList<Jump>> = mutableMapOf()
  protected val scopeToJumpSpot: MutableMap<String, Int> = mutableMapOf()
  private val scopesWithOwnList = mutableSetOf<String>()

  override fun getJump(scopeId: String, count: Int): Jump? {
    inheritProjectListIfNeeded(scopeId)
    lastJumpTimeStamp = System.currentTimeMillis() + JUMP_NAVIGATION_SUPPRESS_MS
    val jumps = scopeToJumps[scopeId] ?: mutableListOf()
    scopeToJumpSpot.putIfAbsent(scopeId, -1)
    val index = jumps.size - 1 - (scopeToJumpSpot[scopeId]!! - count)
    return jumps.getOrNull(index)?.also {
      scopeToJumpSpot[scopeId] = scopeToJumpSpot[scopeId]!! - count
    }
  }

  override fun getJumps(scopeId: String): List<Jump> {
    inheritProjectListIfNeeded(scopeId)
    return scopeToJumps[scopeId] ?: emptyList()
  }

  override fun getJumpSpot(scopeId: String): Int {
    inheritProjectListIfNeeded(scopeId)
    return scopeToJumpSpot[scopeId] ?: -1
  }

  override fun addJump(scopeId: String, jump: Jump, reset: Boolean) {
    inheritProjectListIfNeeded(scopeId)
    lastJumpTimeStamp = System.currentTimeMillis() + JUMP_NAVIGATION_SUPPRESS_MS
    val jumps = scopeToJumps[scopeId] ?: mutableListOf()
    jumps.removeIf { it.filepath == jump.filepath && it.line == jump.line }
    jumps.add(jump)
    putAsMostRecentlyUsed(scopeId, jumps)

    scopeToJumpSpot[scopeId] = if (reset) -1 else (scopeToJumpSpot[scopeId] ?: -1) + 1

    if (jumps.size > SAVE_JUMP_COUNT) {
      jumps.removeFirst()
    }
  }

  override fun saveJumpLocation(editor: VimEditor) {
    addJump(editor, true)
    injector.markService.setMark(editor, '\'')
    includeCurrentCommandAsNavigation(editor)
  }

  override fun removeJump(scopeId: String, jump: Jump) {
    scopeToJumps[scopeId]?.removeIf { it == jump }
  }

  override fun dropLastJump(scopeId: String) {
    scopeToJumps[scopeId]?.removeLastOrNull()
  }

  override fun clearJumps(scopeId: String) {
    // An emptied list is still the scope's own, so it must not inherit the project's again
    scopesWithOwnList.add(scopeId)
    scopeToJumps.remove(scopeId)
    scopeToJumpSpot.remove(scopeId)
  }

  override fun copyJumps(fromId: String, toId: String) {
    val jumps = scopeToJumps[fromId] ?: return
    putAsMostRecentlyUsed(toId, jumps.toMutableList())
    scopeToJumpSpot[toId] = scopeToJumpSpot[fromId] ?: -1
  }

  override fun inheritJumps(fromId: String, toId: String) {
    if (toId in scopesWithOwnList || scopeToJumps.containsKey(toId)) return
    // An inherited empty list is the window's own list, as in Vim - not an absence to be filled in later
    scopesWithOwnList.add(toId)
    scopeToJumps[toId] = scopeToJumps[fromId]?.toMutableList() ?: mutableListOf()
    scopeToJumpSpot[toId] = scopeToJumpSpot[fromId] ?: -1
  }

  override fun updateJumpsFromInsert(scopeId: String, startOffset: Int, length: Int) {
    TODO("Not yet implemented")
  }

  override fun updateJumpsFromDelete(scopeId: String, startOffset: Int, length: Int) {
    TODO("Not yet implemented")
  }

  override fun resetJumps() {
    scopeToJumps.clear()
    scopeToJumpSpot.clear()
    scopesWithOwnList.clear()
  }

  /**
   * Gives a window scope the project's list the first time it is used
   *
   * The list read from disk can only be stored per project - at startup there are no windows yet - so a window that has
   * never had a list of its own starts from it, the same way a new window inherits from the window it was split from.
   */
  private fun inheritProjectListIfNeeded(scopeId: String) {
    if (scopeId in scopesWithOwnList) return
    scopesWithOwnList.add(scopeId)
    if (scopeToJumps.containsKey(scopeId)) return

    val projectId = VimWindowIdService.projectIdOf(scopeId)
    if (projectId != scopeId) {
      copyJumps(projectId, scopeId)
    }
  }

  /** The map iterates in this order, and the list that comes last is the one persisted for the project */
  private fun putAsMostRecentlyUsed(scopeId: String, jumps: MutableList<Jump>) {
    scopeToJumps.remove(scopeId)
    scopeToJumps[scopeId] = jumps
  }

  companion object {
    const val SAVE_JUMP_COUNT: Int = 100
    private const val JUMP_NAVIGATION_SUPPRESS_MS: Long = 500
  }
}
