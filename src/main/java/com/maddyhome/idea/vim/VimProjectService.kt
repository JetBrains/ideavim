/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel

@Service(Service.Level.PROJECT)
internal class VimProjectService(val project: Project) : Disposable {
  override fun dispose() {
    // Not sure if this is a best solution
    ExEntryPanel.instance?.setEditor(null)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): VimProjectService = project.service()
  }
}

@Suppress("unused")
internal val Project.vimDisposable
  get() = VimProjectService.getInstance(this)
