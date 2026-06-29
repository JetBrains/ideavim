/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.undo

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor

/**
 * Implements Vim's "U" command ("undo line"). See `u_saveline` / `u_undoline` in Neovim's `undo.c`.
 *
 * Unlike `u`, `U` does not walk the undo tree. Vim keeps a single saved copy of one line and `U`
 * swaps that copy with the current line, reverting *all* changes made to it since it was first
 * touched (and a second `U` toggles back).
 */
interface LineChange {

  /**
   * Saves the pristine text of [line] so it can later be restored by [undoLineChange] (mirrors
   * `u_saveline`). Must be called *before* the line is modified. While the same line keeps being
   * edited the original snapshot is preserved, which is what makes `U` revert every change on it.
   *
   * @return `true` if a new snapshot was taken, `false` if the line was already saved or invalid.
   */
  fun snapshotLine(line: Int, editor: VimEditor): Boolean

  /**
   * Restores the saved line, swapping it with the current line content (mirrors `u_undoline`).
   * @return `false` when there is nothing to restore, so the caller can beep like Vim does.
   */
  fun undoLineChange(editor: VimEditor, context: ExecutionContext): Boolean
}