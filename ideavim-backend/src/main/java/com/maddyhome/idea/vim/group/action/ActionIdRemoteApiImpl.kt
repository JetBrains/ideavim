/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.action

/**
 * RPC handler for [ActionIdRemoteApi]. Stores the frontend's actions for the ideavimrc completion.
 */
internal class ActionIdRemoteApiImpl : ActionIdRemoteApi {
  override suspend fun setFrontendActions(actions: List<ActionIdInfo>) {
    FrontendActionIds.getInstance().actions = actions.associateBy { it.id }
  }
}
