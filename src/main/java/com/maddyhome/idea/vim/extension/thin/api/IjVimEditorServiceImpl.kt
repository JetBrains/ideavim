/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.listener.VimListenerManager.VimLastSelectedEditorTracker
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimEditorService

class IjVimEditorServiceImpl : VimEditorService {
  override fun getFocusedEditor(): VimEditor? {
    // todo: probably we should use currently focused project
    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: ProjectManager.getInstance().defaultProject
    return VimLastSelectedEditorTracker.getLastSelectedEditor(project)?.vim
  }
}