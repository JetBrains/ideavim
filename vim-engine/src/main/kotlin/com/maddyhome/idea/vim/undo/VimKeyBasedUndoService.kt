/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.undo

/**
 * Undo service for Fleet-like IDEs, where undo log is based on keys
 */
// TODO provide key description for logging
interface VimKeyBasedUndoService : VimUndoRedo {
  fun setMergeUndoKey()

  /**
   * Updates non-insert mode undo key
   */
  fun updateNonMergeUndoKey()

  /**
   * Sets an undo key for which changes in insert mode are usually grouped
   * Setting refresh to true creates a new key and splits the undo sequence (e.g. <C-G>u)
   */
  fun setInsertNonMergeUndoKey(refresh: Boolean = false)
}