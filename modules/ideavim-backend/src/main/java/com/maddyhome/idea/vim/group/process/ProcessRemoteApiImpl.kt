/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.process

import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimProcessGroup

/**
 * RPC handler for [ProcessRemoteApi].
 * Instantiated by [ProcessRemoteApiProvider] during extension registration.
 * Delegates to [ProcessGroup] lazily — the service lookup happens when the
 * RPC call arrives, not when the provider is registered.
 *
 * The downcast to [ProcessGroup] is required because the RPC-specific
 * [ProcessGroup.executeCommand] overload (with explicit shell options) is not
 * on the [VimProcessGroup] interface. This is safe: on the backend,
 * [ProcessGroup] is always the [VimProcessGroup] implementation.
 */
internal class ProcessRemoteApiImpl : ProcessRemoteApi {

  private val processGroup: ProcessGroup
    get() = service<VimProcessGroup>() as ProcessGroup

  override suspend fun executeCommand(
    command: String,
    input: String?,
    currentDirectoryPath: String?,
    shell: String,
    shellcmdflag: String,
    shellxescape: String,
    shellxquote: String,
  ): ProcessResult {
    return processGroup.executeCommand(
      command, input, currentDirectoryPath,
      shell, shellcmdflag, shellxescape, shellxquote,
    )
  }
}
