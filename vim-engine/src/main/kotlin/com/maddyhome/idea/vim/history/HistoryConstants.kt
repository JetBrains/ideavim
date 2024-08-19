/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.history

@Deprecated("Please use VimHistory.Type")
object HistoryConstants {
  @JvmField
  val SEARCH: String = "search"

  @JvmField
  val COMMAND: String = "cmd"

  @JvmField
  val EXPRESSION: String = "expr"

  @JvmField
  val INPUT: String = "input"
}
