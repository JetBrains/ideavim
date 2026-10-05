/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.action

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for sharing the frontend's actions with the backend.
 *
 * In split mode, completion in the ideavimrc editor runs on the backend, whose ActionManager doesn't know the
 * frontend-only actions. `:action` and `<Action>(...)` are executed on the frontend, so the frontend sends its action
 * IDs, with texts and descriptions for the lookup, and the backend offers them in completion alongside its own.
 * See [FrontendActionIds].
 */
@Rpc
@ApiStatus.Internal
interface ActionIdRemoteApi : RemoteApi<Unit> {
  suspend fun setFrontendActions(actions: List<ActionIdInfo>)

  companion object {
    @JvmStatic
    suspend fun getInstance(): ActionIdRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<ActionIdRemoteApi>())
    }
  }
}
