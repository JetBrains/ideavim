/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimProcessGroupBase
import kotlinx.coroutines.runBlocking

/**
 * Thin-client [VimProcessGroup][com.maddyhome.idea.vim.api.VimProcessGroup] for split (Remote Development) mode.
 *
 * Forwards shell command execution to the backend via [ProcessRemoteApi] RPC,
 * where the real [ProcessGroup] runs the command in the backend's environment.
 *
 * In monolith mode this class is never loaded — the backend's [ProcessGroup]
 * is used directly instead.
 */
internal class ProcessGroupSplitClient : VimProcessGroupBase() {

  override fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?,
  ): String? {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) {
      ProcessRemoteApi.getInstance().executeCommand(command, input?.toString(), currentDirectoryPath)
    }
  }
}
