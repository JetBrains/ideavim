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
  private val seededScopes = mutableSetOf<String>()

  override fun getJump(scopeId: String, count: Int): Jump? {
    ensureSeeded(scopeId)
    lastJumpTimeStamp = System.currentTimeMillis() + JUMP_NAVIGATION_SUPPRESS_MS
    val jumps = scopeToJumps[scopeId] ?: mutableListOf()
    scopeToJumpSpot.putIfAbsent(scopeId, -1)
    val index = jumps.size - 1 - (scopeToJumpSpot[scopeId]!! - count)
    return jumps.getOrNull(index)?.also {
      scopeToJumpSpot[scopeId] = scopeToJumpSpot[scopeId]!! - count
    }
  }

  override fun getJumps(scopeId: String): List<Jump> {
    ensureSeeded(scopeId)
    return scopeToJumps[scopeId] ?: emptyList()
  }

  override fun getJumpSpot(scopeId: String): Int {
    ensureSeeded(scopeId)
    return scopeToJumpSpot[scopeId] ?: -1
  }

  override fun addJump(scopeId: String, jump: Jump, reset: Boolean) {
    ensureSeeded(scopeId)
    lastJumpTimeStamp = System.currentTimeMillis() + JUMP_NAVIGATION_SUPPRESS_MS
    // Remove and re-insert, so that the map iterates from least to most recently updated scope
    val jumps = scopeToJumps.remove(scopeId) ?: mutableListOf()
    jumps.removeIf { it.filepath == jump.filepath && it.line == jump.line }
    jumps.add(jump)
    scopeToJumps[scopeId] = jumps

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
    seededScopes.add(scopeId)
    scopeToJumps.remove(scopeId)
    scopeToJumpSpot.remove(scopeId)
  }

  override fun copyJumps(fromId: String, toId: String) {
    val jumps = scopeToJumps[fromId] ?: return
    scopeToJumps[toId] = jumps.toMutableList()
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
  }

  private fun ensureSeeded(scopeId: String) {
    if (!seededScopes.add(scopeId)) return
    if (scopeToJumps.containsKey(scopeId)) return
    val projectId = scopeId.substringBeforeLast(VimWindowIdService.WINDOW_SCOPE_SEPARATOR)
    if (projectId == scopeId) return
    copyJumps(projectId, scopeId)
  }

  companion object {
    const val SAVE_JUMP_COUNT: Int = 100
    private const val JUMP_NAVIGATION_SUPPRESS_MS: Long = 500
  }
}
