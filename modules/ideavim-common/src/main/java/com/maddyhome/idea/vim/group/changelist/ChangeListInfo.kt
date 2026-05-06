/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.changelist

import com.intellij.platform.rpc.topics.ProjectRemoteTopic
import kotlinx.serialization.Serializable

/**
 * Backend-to-frontend change-list event. Line/col are pre-computed on the
 * backend so the frontend doesn't need a VirtualFile lookup -- which can race
 * with editor loading in split mode (mirrors the `JumpInfo` pattern).
 */
@Serializable
data class ChangeListInfo(
  val line: Int,
  val col: Int,
  val filepath: String,
  val protocol: String,
  val timestamp: Long,
)

val CHANGE_LIST_REMOTE_TOPIC: ProjectRemoteTopic<ChangeListInfo> =
  ProjectRemoteTopic("ideavim.changelist", ChangeListInfo.serializer())
