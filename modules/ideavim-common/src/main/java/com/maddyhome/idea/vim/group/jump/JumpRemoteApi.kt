/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for jump operations that require backend-only APIs.
 * Called by [VimJumpServiceSplitClient] in split mode.
 *
 * [includeCurrentCommandAsNavigation] forwards [IdeDocumentHistory] calls to the backend.
 * [getListenerJumps] fetches jumps collected by [JumpsListener] on the backend
 * (IDE navigation events) so the frontend can merge them with its local jump list.
 */
@Rpc
@ApiStatus.Internal
interface JumpRemoteApi : RemoteApi<Unit> {
  suspend fun includeCurrentCommandAsNavigation(projectId: String?)

  /**
   * Returns jumps collected by [JumpsListener] on the backend for the given project.
   * These are IDE-initiated navigation events (e.g., Go to Declaration, Recent Places)
   * that only fire on the backend.
   */
  suspend fun getListenerJumps(projectId: String?): List<JumpInfo>

  companion object {
    @JvmStatic
    suspend fun getInstance(): JumpRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<JumpRemoteApi>())
    }
  }
}
