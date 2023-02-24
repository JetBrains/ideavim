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
interface VimJumpService {
  var lastJumpTimeStamp: Long
  
  fun includeCurrentCommandAsNavigation(editor: VimEditor)
  fun getJumpSpot(): Int
  fun getJump(count: Int): Jump?
  fun getJumps(): List<Jump>
  fun addJump(jump: Jump, reset: Boolean)
  fun addJump(editor: VimEditor, reset: Boolean)
  fun saveJumpLocation(editor: VimEditor)
  fun removeJump(jump: Jump)
  fun dropLastJump()
  fun updateJumpsFromInsert(editor: VimEditor, startOffset: Int, length: Int)
  fun updateJumpsFromDelete(editor: VimEditor, startOffset: Int, length: Int)
  fun resetJumps()
}
