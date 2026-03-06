/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import kotlinx.serialization.Serializable

/**
 * Serializable data transfer object for bookmark information over RPC.
 * Returned by [BookmarkRemoteApi.createOrGetSystemMark] to confirm bookmark creation.
 */
@Serializable
data class BookmarkInfo(
  val key: Char,
  val line: Int,
  val col: Int,
  val filepath: String,
  val protocol: String,
)
