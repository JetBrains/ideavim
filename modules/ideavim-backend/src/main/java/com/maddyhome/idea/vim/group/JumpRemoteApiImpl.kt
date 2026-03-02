/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory

/**
 * RPC handler for [JumpRemoteApi].
 * Instantiated by [JumpRemoteApiProvider] during extension registration.
 * Delegates to [IdeDocumentHistory] on the backend where it is available.
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
}
