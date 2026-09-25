/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimIndentConfig {
  fun getIndentSize(depth: Int): Int
  fun createIndentByDepth(depth: Int): String

  /**
   * size in spaces
   */
  fun createIndentBySize(size: Int): String

  /**
   * Whether the IDE has been asked to keep the indent of a line that holds nothing else.
   *
   * Vim deletes such an indent when Insert mode is left without anything being typed, so that `o<Esc>` leaves an empty
   * line rather than one full of white space. "Keep indents on empty lines" in Settings | Editor | Code Style asks for
   * the opposite. It is off by default, so Vim's behaviour stands until the user has said otherwise.
   */
  val keepIndentsOnEmptyLines: Boolean
    get() = false
}