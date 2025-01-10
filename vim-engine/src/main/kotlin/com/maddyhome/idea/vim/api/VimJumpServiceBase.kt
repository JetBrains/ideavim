/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump

abstract class VimJumpServiceBase : VimJumpService {
  protected val projectToJumps: MutableMap<String, MutableList<Jump>> = mutableMapOf()
  protected val projectToJumpSpot: MutableMap<String, Int> = mutableMapOf()

  override fun getJump(projectId: String, count: Int): Jump? {
    val jumps = projectToJumps[projectId] ?: mutableListOf()
    projectToJumpSpot.putIfAbsent(projectId, -1)
    val index = jumps.size - 1 - (projectToJumpSpot[projectId]!! - count)
    return jumps.getOrNull(index)?.also {
      projectToJumpSpot[projectId] = projectToJumpSpot[projectId]!! - count
    }
  }

  override fun getJumps(projectId: String): List<Jump> {
    return projectToJumps[projectId] ?: emptyList()
  }

  override fun getJumpSpot(projectId: String): Int {
    return projectToJumpSpot[projectId] ?: -1
  }

  override fun addJump(projectId: String, jump: Jump, reset: Boolean) {
    lastJumpTimeStamp = System.currentTimeMillis()
    val jumps = projectToJumps.getOrPut(projectId) { mutableListOf() }
    jumps.removeIf { it.filepath == jump.filepath && it.line == jump.line }
    jumps.add(jump)

    projectToJumpSpot[projectId] = if (reset) -1 else (projectToJumpSpot[projectId] ?: -1) + 1

    if (jumps.size > SAVE_JUMP_COUNT) {
      jumps.removeFirst()
    }
  }

  override fun saveJumpLocation(editor: VimEditor) {
    addJump(editor, true)
    injector.markService.setMark(editor, '\'')
    includeCurrentCommandAsNavigation(editor)
  }

  override fun removeJump(projectId: String, jump: Jump) {
    projectToJumps[projectId]?.removeIf { it == jump }
  }

  override fun dropLastJump(projectId: String) {
    projectToJumps[projectId]?.removeLastOrNull()
  }

  override fun clearJumps(projectId: String) {
    projectToJumps.remove(projectId)
    projectToJumpSpot.remove(projectId)
  }

  override fun updateJumpsFromInsert(projectId: String, startOffset: Int, length: Int) {
    TODO("Not yet implemented")
  }

  override fun updateJumpsFromDelete(projectId: String, startOffset: Int, length: Int) {
    TODO("Not yet implemented")
  }

  override fun resetJumps() {
    projectToJumps.clear()
    projectToJumpSpot.clear()
  }

  companion object {
    const val SAVE_JUMP_COUNT: Int = 100
  }
}
