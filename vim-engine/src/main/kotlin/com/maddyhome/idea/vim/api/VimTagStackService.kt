/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump
import org.jetbrains.annotations.TestOnly

/**
 * Manages the tag stack, which records positions from which tag jumps were made.
 *
 * Ctrl-] (GotoDeclaration) pushes the current position onto the stack.
 * Ctrl-T pops from the stack and navigates to the popped position.
 *
 * This is separate from the jump list (Ctrl-O/Ctrl-I), which tracks all navigation.
 * The tag stack only tracks tag jumps.
 */
interface VimTagStackService {
  /** Push the current caret position onto the tag stack for the given editor's project. */
  fun pushTag(editor: VimEditor)

  /**
   * Pop [count] entries from the tag stack and return the position to navigate to.
   * Returns null if the stack has fewer than [count] entries.
   */
  fun popTag(editor: VimEditor, count: Int): Jump?

  /** Return all entries in the tag stack, oldest first. */
  fun getTagStack(editor: VimEditor): List<Jump>

  @TestOnly
  fun resetTagStack()
}
