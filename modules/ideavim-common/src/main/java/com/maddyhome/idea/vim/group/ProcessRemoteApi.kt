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
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

/**
 * Serializable result of a shell command execution over RPC.
 * Carries both the command output and any status info (exit code)
 * so the frontend can display appropriate messages.
 */
@Serializable
data class ProcessResult(
  val output: String? = null,
  val exitCode: Int? = null,
)

/**
 * RPC interface for executing shell commands on the backend.
 * Called directly by Ex commands (`:!`, `:read !cmd`) to forward
 * shell execution to the backend where the real [ProcessGroup] runs.
 *
 * Shell options (`shell`, `shellcmdflag`, etc.) are passed from the frontend
 * because `injector` (which holds Vim options) is not initialized on the backend.
 */
@Rpc
@ApiStatus.Internal
interface ProcessRemoteApi : RemoteApi<Unit> {
  suspend fun executeCommand(
    command: String,
    input: String?,
    currentDirectoryPath: String?,
    shell: String,
    shellcmdflag: String,
    shellxescape: String,
    shellxquote: String,
  ): ProcessResult

  companion object {
    @JvmStatic
    suspend fun getInstance(): ProcessRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<ProcessRemoteApi>())
    }
  }
}
