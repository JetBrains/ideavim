/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.command.MappingMode

data class AbbreviationEntry(
  val lhs: String,
  val rhs: String,
  val modes: Set<MappingMode>,
  val recursive: Boolean,
  val expression: Boolean,
)

