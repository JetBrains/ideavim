/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener

internal object WindowJumpsChangeListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    val perWindow = injector.globalOptions().ideawindowjumps
    for (project in ProjectManager.getInstance().openProjects) {
      if (project.isDisposed) continue
      if (perWindow) {
        seedWindowsFromProject(project)
      } else {
        collapseToFocusedWindow(project)
      }
    }
  }

  private fun seedWindowsFromProject(project: Project) {
    val projectId = injector.file.getProjectId(project)
    for (window in FileEditorManagerEx.getInstanceEx(project).windows) {
      val scopeId = windowScopeId(window) ?: continue
      injector.jumpService.copyJumps(projectId, scopeId)
    }
  }

  private fun collapseToFocusedWindow(project: Project) {
    val projectId = injector.file.getProjectId(project)
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val focusedScopeId = fileEditorManager.currentWindow?.let { windowScopeId(it) }

    if (focusedScopeId != null) {
      if (injector.jumpService.getJumps(focusedScopeId).isNotEmpty()) {
        injector.jumpService.copyJumps(focusedScopeId, projectId)
      } else {
        injector.jumpService.clearJumps(projectId)
      }
    }

    for (window in fileEditorManager.windows) {
      val scopeId = windowScopeId(window) ?: continue
      injector.jumpService.clearJumps(scopeId)
    }
  }

  private fun windowScopeId(window: EditorWindow): String? {
    val editor = window.selectedEditor() ?: return null
    return injector.windowIdService.getWindowScopeId(editor.vim)
  }

  private fun EditorWindow.selectedEditor(): Editor? {
    return (selectedComposite?.selectedEditor as? TextEditor)?.editor
  }
}
