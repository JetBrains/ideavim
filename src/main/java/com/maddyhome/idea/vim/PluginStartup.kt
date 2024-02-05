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
import com.intellij.openapi.startup.ProjectActivity
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions

/**
 * @author Alex Plate
 */
internal class PluginStartup : ProjectActivity/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  override suspend fun execute(project: Project) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    // This code should be executed once
    VimPlugin.getInstance().initialize()
  }
}

// This is a temporal workaround for VIM-2487
internal class PyNotebooksCloseWorkaround : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    // TODO: Confirm context in CWM scenario
    if (injector.globalIjOptions().closenotebooks) {
      injector.editorGroup.getEditors().forEach { vimEditor ->
        val editor = (vimEditor as IjVimEditor).editor
        val virtualFile = EditorHelper.getVirtualFile(editor)
        if (virtualFile?.extension == "ipynb") {
          val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
          fileEditorManager.closeFile(virtualFile)
        }
      }
    }
  }
}
