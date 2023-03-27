/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump

public abstract class VimJumpServiceBase : VimJumpService {
  @JvmField
  protected val jumps: MutableList<Jump> = ArrayList() // todo should it be mutable?

  @JvmField
  protected var jumpSpot: Int = -1

  override fun getJumpSpot(): Int {
    return jumpSpot
  }

  override fun getJump(count: Int): Jump? {
    val index = jumps.size - 1 - (jumpSpot - count)
    return if (index < 0 || index >= jumps.size) {
      null
    } else {
      jumpSpot -= count
      jumps[index]
    }
  }

  override fun getJumps(): List<Jump> {
    return jumps
  }

  override fun addJump(jump: Jump, reset: Boolean) {
    lastJumpTimeStamp = System.currentTimeMillis()
    val filename = jump.filepath

    for (i in jumps.indices) {
      val j = jumps[i]
      if (filename == j.filepath && j.line == jump.line) {
        jumps.removeAt(i)
        break
      }
    }

    jumps.add(jump)

    if (reset) {
      jumpSpot = -1
    } else {
      jumpSpot++
    }

    if (jumps.size > SAVE_JUMP_COUNT) {
      jumps.removeAt(0)
    }
  }

  override fun saveJumpLocation(editor: VimEditor) {
    addJump(editor, true)
    injector.markService.setMark(editor, '\'')
    includeCurrentCommandAsNavigation(editor)
  }

  override fun removeJump(jump: Jump) {
    val lastIndex = jumps.withIndex().findLast { it.value == jump }?.index ?: return
    jumps.removeAt(lastIndex)
  }

  override fun dropLastJump() {
    jumps.removeLast()
  }

  override fun updateJumpsFromInsert(editor: VimEditor, startOffset: Int, length: Int) {
    TODO("Not yet implemented")
  }

  override fun updateJumpsFromDelete(editor: VimEditor, startOffset: Int, length: Int) {
    TODO("Not yet implemented")
  }

  override fun resetJumps() {
    jumps.clear()
    jumpSpot = -1
  }

  public companion object {
    public const val SAVE_JUMP_COUNT: Int = 100
  }
}
