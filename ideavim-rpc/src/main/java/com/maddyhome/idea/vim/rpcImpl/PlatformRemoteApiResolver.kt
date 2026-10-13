/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rpcImpl

import com.intellij.platform.rpc.RemoteApiProviderService
import com.maddyhome.idea.vim.group.rpc.VimRemoteApiResolver
import fleet.rpc.RemoteApi
import fleet.rpc.RemoteApiDescriptor

class PlatformRemoteApiResolver : VimRemoteApiResolver {
  override suspend fun <T : RemoteApi<Unit>> resolve(descriptor: RemoteApiDescriptor<T>): T {
    return RemoteApiProviderService.resolve(descriptor)
  }
}
