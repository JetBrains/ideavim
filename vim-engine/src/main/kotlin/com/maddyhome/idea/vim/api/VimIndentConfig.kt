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
}