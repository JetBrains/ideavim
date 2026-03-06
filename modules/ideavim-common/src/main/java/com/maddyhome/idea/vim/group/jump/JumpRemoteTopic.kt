/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.platform.rpc.topics.ProjectRemoteTopic

/**
 * Topic for broadcasting jump events from backend to frontend.
 *
 * The backend [JumpsListener] broadcasts [JumpInfo] events here when
 * IDE navigation events (Go to Declaration, Recent Places, etc.) occur.
 * The frontend [JumpRemoteTopicListener] receives them and adds/removes
 * jumps via [com.maddyhome.idea.vim.api.VimJumpService].
 */
val JUMP_REMOTE_TOPIC: ProjectRemoteTopic<JumpInfo> =
  ProjectRemoteTopic("ideavim.jump", JumpInfo.serializer())
