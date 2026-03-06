/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.initInjector

// This is a temporal workaround for VIM-2487
internal class PyNotebooksCloseWorkaround : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    initInjector()
    // TODO: Confirm context in CWM scenario
    if (injector.globalIjOptions().closenotebooks) {
      injector.editorGroup.getEditors().forEach { vimEditor ->
        val editor = vimEditor.ij
        val virtualFile = EditorHelper.getVirtualFile(editor)
        if (virtualFile?.extension == "ipynb") {
          val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
          fileEditorManager.closeFile(virtualFile)
        }
      }
    }
  }
}
