/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * The quickfix list: places filled by a command like `:Ggrep` and walked with `:cnext`.
 *
 * Like Vim, there is a single list for the whole application, and it is not persisted.
 */
interface QuickFixService {
  val size: Int

  /**
   * 0-based index of the current item. Meaningless while the list is empty.
   */
  val currentIndex: Int

  val currentItem: QuickFixItem?

  /**
   * Replaces the list and makes its first item current.
   */
  fun replaceList(items: List<QuickFixItem>)

  /**
   * Moves up to [count] items forward, stopping at the last one.
   *
   * @return false if the current item already is the last one
   */
  fun moveForward(count: Int): Boolean

  /**
   * Opens the file of the current item and puts the caret on it, recording a jump first.
   *
   * @return the editor showing the item, or null if there is no current item or its file can't be opened
   */
  fun jumpToCurrentItem(editor: VimEditor): VimEditor?

  fun reset()
}

/**
 * @param line 0-based line
 * @param col 0-based character column
 * @param text the text shown for the item, e.g. the matching line
 */
data class QuickFixItem(val line: Int, val col: Int, val filepath: String, val protocol: String, val text: String)
