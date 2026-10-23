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
 * Every project has its own list. The project is the one of the editor passed in.
 * Lists are not persisted.
 * Every jump records the position it leaves in the jump list.
 */
interface QuickFixService {
  /**
   * Replaces the list of the editor's project. Its first item becomes the current one, without jumping to it.
   */
  fun replaceList(editor: VimEditor, items: List<QuickFixItem>)

  /**
   * Jumps to the first item, like `:cfirst`.
   */
  fun jumpToFirst(editor: VimEditor): QuickFixJump

  /**
   * Jumps [count] items forward, like `:cnext`. A count past the end stops at the last item.
   */
  fun jumpForward(editor: VimEditor, count: Int): QuickFixJump

  /**
   * Jumps [count] items backward, like `:cprevious`. A count past the start stops at the first item.
   */
  fun jumpBackward(editor: VimEditor, count: Int): QuickFixJump

  /**
   * Jumps to the first item of the [count]th next file, like `:cnfile`. A count past the last
   * file stops at the first item of the last file.
   */
  fun jumpToNextFile(editor: VimEditor, count: Int): QuickFixJump

  /**
   * Jumps to the last item of the [count]th previous file, like `:cpfile`. A count past the first
   * file stops at the last item of the first file.
   */
  fun jumpToPreviousFile(editor: VimEditor, count: Int): QuickFixJump

  /**
   * Forgets the list of a project, e.g. when it's closed.
   */
  fun removeList(projectId: String)

  /**
   * Forgets the lists of all projects.
   */
  fun reset()
}

/**
 * @param line 0-based line
 * @param col 0-based character column
 * @param text the text shown for the item, e.g. the matching line
 */
data class QuickFixItem(val line: Int, val col: Int, val filepath: String, val protocol: String, val text: String)

sealed interface QuickFixJump {
  data object EmptyList : QuickFixJump

  /**
   * The current item already is the last one (or the first one, when going backward).
   */
  data object NoMoreItems : QuickFixJump

  /**
   * [item] became the current one.
   *
   * @param editor the editor showing [item], or null if its file couldn't be opened
   * @param number 1-based position of [item] in the list
   * @param size number of items in the list
   */
  data class Moved(val editor: VimEditor?, val item: QuickFixItem, val number: Int, val size: Int) : QuickFixJump
}
