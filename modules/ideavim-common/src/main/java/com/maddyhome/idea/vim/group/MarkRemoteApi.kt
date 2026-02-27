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
 * RPC interface for mark operations on the backend.
 * Called by [VimMarkServiceSplitClient] in thin-client mode to forward
 * mark operations to the backend where [VimMarkServiceImpl] manages
 * persistence and IDE bookmark integration.
 */
@Rpc
@ApiStatus.Internal
interface MarkRemoteApi : RemoteApi<Unit> {
  suspend fun getMark(projectBasePath: String?, char: Char): MarkInfo?
  suspend fun setMark(
    projectBasePath: String?,
    char: Char,
    filePath: String,
    line: Int,
    col: Int,
    protocol: String,
  ): Boolean

  suspend fun removeMark(projectBasePath: String?, char: Char)
  suspend fun getMarks(projectBasePath: String?): List<MarkInfo>

  companion object {
    @JvmStatic
    suspend fun getInstance(): MarkRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<MarkRemoteApi>())
    }
  }
}
