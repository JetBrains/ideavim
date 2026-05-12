/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import kotlinx.serialization.Serializable

/**
 * Serializable event for jump changes, sent via [JumpRemoteTopic]
 * from the backend [JumpsListener] to the frontend.
 */
@Serializable
data class JumpInfo(
  val line: Int,
  val col: Int,
  val filepath: String,
  val protocol: String,
  val added: Boolean,
  val timestamp: Long,
)
