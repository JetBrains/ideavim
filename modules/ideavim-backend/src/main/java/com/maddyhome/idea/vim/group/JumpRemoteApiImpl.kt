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

/**
 * RPC handler for [JumpRemoteApi].
 * Instantiated by [JumpRemoteApiProvider] during extension registration.
 * Delegates to [IdeDocumentHistory] on the backend where it is available.
 */
internal class JumpRemoteApiImpl : JumpRemoteApi {
  override suspend fun includeCurrentCommandAsNavigation(projectBasePath: String?) {
    if (projectBasePath == null) return
    val project = ProjectManager.getInstance().openProjects.firstOrNull { it.basePath == projectBasePath } ?: return
    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
  }
}
