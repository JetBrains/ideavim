/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.process

import com.intellij.openapi.components.service

/**
 * RPC handler for [ProcessRemoteApi].
 * Delegates to [ProcessGroup] for shell command execution on the backend.
 */
internal class ProcessRemoteApiImpl : ProcessRemoteApi {

  private val processGroup: ProcessGroup
    get() = service()

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
