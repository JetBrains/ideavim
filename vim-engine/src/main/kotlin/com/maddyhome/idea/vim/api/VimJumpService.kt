/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump

// todo should it be multicaret?
// todo docs
public interface VimJumpService {
  /**
   * Timestamp (`System.currentTimeMillis()`) of the last Jump command <C-o>, <C-i>
   * it's a temporary sticky tape to avoid difficulties with Platform, which counts <C-o>, <C-i> as new jump locations
   * and messes up our jump list
   */
  public var lastJumpTimeStamp: Long

  public fun includeCurrentCommandAsNavigation(editor: VimEditor)
  public fun getJumpSpot(): Int
  public fun getJump(count: Int): Jump?
  public fun getJumps(): List<Jump>
  public fun addJump(jump: Jump, reset: Boolean)
  public fun saveJumpLocation(editor: VimEditor)
  public fun removeJump(jump: Jump)
  public fun dropLastJump()
  public fun updateJumpsFromInsert(editor: VimEditor, startOffset: Int, length: Int)
  public fun updateJumpsFromDelete(editor: VimEditor, startOffset: Int, length: Int)
  public fun resetJumps()
}

public fun VimJumpService.addJump(editor: VimEditor, reset: Boolean) {
  val path = editor.getPath() ?: return
  val position = editor.offsetToBufferPosition(editor.currentCaret().offset.point)
  val jump = Jump(position.line, position.column, path)
  addJump(jump, reset)
}
