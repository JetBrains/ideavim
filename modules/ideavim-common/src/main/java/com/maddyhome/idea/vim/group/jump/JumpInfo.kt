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
 * Serializable data transfer object for jump information over RPC.
 * Returned by [JumpRemoteApi.getListenerJumps] to transfer backend jump list entries
 * (collected by [JumpsListener]) to the frontend.
 */
@Serializable
data class JumpInfo(
  val line: Int,
  val col: Int,
  val filepath: String,
  val protocol: String,
)
