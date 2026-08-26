/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.tag

/**
 * One entry of the tag stack, see "h tag-stack"
 *
 * [line], [col], [filepath] and [protocol] are the position the tag jump was made *from* - the place `<C-T>` returns
 * to. [tagName] is the tag that was jumped to, shown in the `TO tag` column of `:tags`.
 */
data class TagStackEntry(
  // Rewritten when `:tag` redoes this jump, so <C-T> comes back to where the redo was made from
  var line: Int,
  var col: Int,
  val filepath: String,
  val protocol: String,
  val tagName: String,
)
