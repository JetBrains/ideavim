/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.localEditors
import com.maddyhome.idea.vim.vimscript.services.IjOptionConstants

/**
 * @author Alex Plate
 */
class PluginStartup : ProjectActivity, DumbAware/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  // TODO: Should we use RunOnceUtil?
  override suspend fun execute(project: Project) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    // This code should be executed once
    VimPlugin.getInstance().initialize()
  }
}

// This is a temporal workaround for VIM-2487
class PyNotebooksCloseWorkaround : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    if (injector.globalOptions().isSet(IjOptionConstants.closenotebooks)) {
      localEditors().forEach { editor ->
        val virtualFile = EditorHelper.getVirtualFile(editor)
        if (virtualFile?.extension == "ipynb") {
          val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
          fileEditorManager.closeFile(virtualFile)
        }
      }
    }
  }
}
