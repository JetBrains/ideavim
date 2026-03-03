/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.maddyhome.idea.vim.group.JumpInfo
import com.maddyhome.idea.vim.group.JumpRemoteApi
import com.maddyhome.idea.vim.group.findProjectById

/**
 * RPC handler for [JumpRemoteApi].
 * Instantiated by [JumpRemoteApiProvider] during extension registration.
 * Delegates to [IdeDocumentHistory] on the backend where it is available.
 *
 * [getListenerJumps] reads from [BackendJumpStorage] to return jumps
 * collected by [JumpsListener] (IDE navigation events on the backend).
 *
 * Note: Must NOT use [com.maddyhome.idea.vim.api.injector] — it is not initialized on the backend
 * in split mode. Uses [findProjectById] from [BackendFileUtil] instead.
 */
internal class JumpRemoteApiImpl : JumpRemoteApi {
  override suspend fun includeCurrentCommandAsNavigation(projectId: String?) {
    if (projectId == null) return
    val project = findProjectById(projectId) ?: return
    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
  }

  override suspend fun getListenerJumps(projectId: String?): List<JumpInfo> {
    if (projectId == null) return emptyList()
    return service<BackendJumpStorage>().getJumps(projectId).map {
      JumpInfo(it.line, it.col, it.filepath, it.protocol)
    }
  }
}
