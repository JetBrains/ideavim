/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.platform.project.projectId
import com.maddyhome.idea.vim.api.QuickFixService

/**
 * Forgets the quickfix list of a closed project, so reopening it starts empty like a new Vim.
 */
internal class QuickFixProjectCloseListener : ProjectManagerListener {
  override fun projectClosed(project: Project) {
    service<QuickFixService>().removeList(project.projectId().serializeToString())
  }
}
