/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.process

import com.maddyhome.idea.vim.api.GlobalOptions
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimProcessGroupBase
import com.maddyhome.idea.vim.group.rpc

/**
 * Unified [VimProcessGroup][com.maddyhome.idea.vim.api.VimProcessGroup] that always uses RPC.
 *
 * Shell command execution is forwarded to the backend where the real process runs.
 * Shell options are passed as RPC parameters (read on the frontend where `injector` is initialized).
 */
internal class IjProcessGroup : VimProcessGroupBase() {

  override fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?,
    options: GlobalOptions,
  ): String? {
    val result = rpc {
      ProcessRemoteApi.getInstance().executeCommand(
        command, input?.toString(), currentDirectoryPath,
        options.shell, options.shellcmdflag, options.shellxescape, options.shellxquote,
      )
    }
    lastExitCode = result.exitCode
    return result.output
  }
}
