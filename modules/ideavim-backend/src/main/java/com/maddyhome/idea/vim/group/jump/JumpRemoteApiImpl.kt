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
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull

/**
 * RPC handler for [JumpRemoteApi].
 * Instantiated by [JumpRemoteApiProvider] during extension registration.
 * Delegates to [IdeDocumentHistory] on the backend where it is available.
 *
 * [getListenerJumps] reads from [BackendJumpStorage] to return jumps
 * collected by [JumpsListener] (IDE navigation events on the backend).
 *
 * Note: Must NOT use [com.maddyhome.idea.vim.api.injector] — it is not initialized on the backend
 * in split mode. Uses platform [ProjectId.findProjectOrNull] instead.
 */
internal class JumpRemoteApiImpl : JumpRemoteApi {
  override suspend fun includeCurrentCommandAsNavigation(projectId: ProjectId?) {
    if (projectId == null) return
    val project = projectId.findProjectOrNull() ?: return
    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
  }

  override suspend fun getListenerJumps(projectId: ProjectId?): List<JumpInfo> {
    if (projectId == null) return emptyList()
    val storageKey = projectId.serializeToString()
    return service<BackendJumpStorage>().getJumps(storageKey).map {
      JumpInfo(it.line, it.col, it.filepath, it.protocol)
    }
  }
}
