/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import kotlinx.serialization.Serializable

/**
 * Serializable data transfer object for mark information over RPC.
 * All fields are primitives, so it is directly serializable by the Fleet RPC framework.
 */
@Serializable
data class MarkInfo(
  val key: Char,
  val line: Int,
  val col: Int,
  val filepath: String,
  val protocol: String,
)
