/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupActivity
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.localEditors
import com.maddyhome.idea.vim.newapi.globalIjOptions

/**
 * @author Alex Plate
 */
// This service should be migrated to ProjectActivity. But we should cariful because simple replacement
// leads to deadlock in tests. I'm not sure about the exact reasons, but "invokeAndWait" inside "initialize" function
// causes this deadlock. Good new: it's easy reproducible in tests.
// Previous migration: fc7efd5484a13b40ba9bf86a1d5429e215d973f3
// Revert: 24dd84b31cffb99eb6114524859a46d02717d33f
internal class PluginStartup : StartupActivity.DumbAware/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  override fun runActivity(project: Project) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    // This code should be executed once
    VimPlugin.getInstance().initialize()
  }
}

// This is a temporal workaround for VIM-2487
internal class PyNotebooksCloseWorkaround : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    if (injector.globalIjOptions().closenotebooks) {
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
