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
 * RPC interface for jump operations that require backend-only APIs.
 * Called by [VimJumpServiceSplitClient] to forward [IdeDocumentHistory] calls
 * to the backend where the real [VimJumpServiceImpl] runs.
 *
 * Only methods that need backend access are exposed here.
 * All other jump operations (state management) run locally via [VimJumpServiceBase].
 */
@Rpc
@ApiStatus.Internal
interface JumpRemoteApi : RemoteApi<Unit> {
  suspend fun includeCurrentCommandAsNavigation(projectId: String?)

  companion object {
    @JvmStatic
    suspend fun getInstance(): JumpRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<JumpRemoteApi>())
    }
  }
}
