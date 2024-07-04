/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimOutputPanel {
  val text: String
  val isShown: Boolean

  fun addText(text: String, isNewLine: Boolean = true)
  fun update()

  /**
   * It's implementation should execute update()
   * This method can be called even for [isShown] panels
   */
  fun show()
  fun close()
}