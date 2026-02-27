/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for executing shell commands on the backend.
 * Called directly by Ex commands (`:!`, `:read !cmd`) to forward
 * shell execution to the backend where the real [ProcessGroup] runs.
 *
 * All parameters are primitives (serializable by default).
 */
@Rpc
@ApiStatus.Internal
interface ProcessRemoteApi : RemoteApi<Unit> {
  suspend fun executeCommand(
    command: String,
    input: String?,
    currentDirectoryPath: String?,
  ): String?

  companion object {
    @JvmStatic
    suspend fun getInstance(): ProcessRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<ProcessRemoteApi>())
    }
  }
}
