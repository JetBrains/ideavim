/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.service
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.group.action.ActionIdInfo
import com.maddyhome.idea.vim.group.action.ActionIdRemoteApi
import kotlinx.coroutines.launch

/**
 * Sends the frontend's actions to the backend, where the ideavimrc completion runs in split mode.
 * See [com.maddyhome.idea.vim.group.action.FrontendActionIds].
 *
 * Called on startup and whenever a plugin is loaded or unloaded, as that changes the set of actions.
 * In monolith this is a no-op: the completion uses the same ActionManager as `:action`.
 */
internal object FrontendActionIdsSync {
  fun sendToBackend() {
    if (!PlatformUtils.isJetBrainsClient()) return

    // Fire-and-forget: called from startup and plugin listeners, which must not block on the backend
    service<CoroutineScopeProvider>().coroutineScope.launch {
      ActionIdRemoteApi.getInstance().setFrontendActions(ActionIdInfo.collect())
    }
  }
}
