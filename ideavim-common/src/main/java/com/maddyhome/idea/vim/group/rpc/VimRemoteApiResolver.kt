/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.rpc

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.RemoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

private val resolverEP = ExtensionPointName.create<VimRemoteApiResolver>("IdeaVIM.remoteApiResolver")

/**
 * Resolves Fleet RPC remote APIs via [RemoteApiProviderService] without making this module depend on the
 * `intellij.platform.rpc` module.
 *
 * That module doesn't exist in 2025.3 (the classes live in the core classloader), is embedded in the core
 * classloader in 2026.1 and 2026.2, and is a standalone module since 2026.3. A required dependency on it from this
 * embedded module would disable the whole plugin on 2025.3, so the dependency lives in the optional
 * `IdeaVIM.ideavim-rpc` module, which registers the resolver whenever the module exists. Without it, we fall back
 * to [CoreRemoteApiResolver], which works as long as [RemoteApiProviderService] is in the core classloader.
 */
@ApiStatus.Internal
interface VimRemoteApiResolver {
  suspend fun <T : RemoteApi<Unit>> resolve(descriptor: RemoteApiDescriptor<T>): T

  companion object {
    suspend fun <T : RemoteApi<Unit>> resolve(descriptor: RemoteApiDescriptor<T>): T {
      val resolver = resolverEP.extensionList.firstOrNull() ?: CoreRemoteApiResolver
      return resolver.resolve(descriptor)
    }
  }
}

/**
 * Same call as in `IdeaVIM.ideavim-rpc`, but [RemoteApiProviderService] is resolved through the plugin's own
 * classloader, so it only works on IDEs where the class is in the core classloader (2026.2 and earlier).
 */
private object CoreRemoteApiResolver : VimRemoteApiResolver {
  override suspend fun <T : RemoteApi<Unit>> resolve(descriptor: RemoteApiDescriptor<T>): T {
    return RemoteApiProviderService.resolve(descriptor)
  }
}
