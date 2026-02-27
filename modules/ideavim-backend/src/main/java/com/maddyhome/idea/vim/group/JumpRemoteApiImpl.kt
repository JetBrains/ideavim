/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.api.injector

/**
 * RPC handler for [JumpRemoteApi].
 * Instantiated by [JumpRemoteApiProvider] during extension registration.
 * Delegates to [IdeDocumentHistory] on the backend where it is available.
 */
internal class JumpRemoteApiImpl : JumpRemoteApi {
  override suspend fun includeCurrentCommandAsNavigation(projectId: String?) {
    if (projectId == null) return
    val project = ProjectManager.getInstance().openProjects
      .firstOrNull { injector.file.getProjectId(it) == projectId } ?: return
    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
  }
}
