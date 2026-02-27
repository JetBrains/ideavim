/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimProcessGroup

/**
 * RPC handler for [ProcessRemoteApi].
 * Instantiated by [ProcessRemoteApiProvider] during extension registration.
 * Delegates to [ProcessGroup] lazily — the service lookup happens when the
 * RPC call arrives, not when the provider is registered.
 */
internal class ProcessRemoteApiImpl : ProcessRemoteApi {
  override suspend fun executeCommand(
    command: String,
    input: String?,
    currentDirectoryPath: String?,
  ): String? {
    return (service<VimProcessGroup>() as ProcessGroup).executeCommand(command, input, currentDirectoryPath)
  }
}
