/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFileBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import kotlinx.coroutines.runBlocking

/**
 * Thin-client [VimFile][com.maddyhome.idea.vim.api.VimFile] for split (Remote Development) mode.
 *
 * Extends [VimFileBase] for pure-engine operations (`displayHexInfo`, `displayLocationInfo`).
 * All other operations are forwarded to the backend via [FileRemoteApi] RPC.
 * The only local work is extracting serializable parameters and showing messages/errors.
 *
 * Note: `projectBasePath` is always `null` because the thin client's project path
 * (sandbox path) differs from the backend's real project path. The backend resolves
 * `null` to its first open project, same as [VimMarkServiceSplitClient].
 */
internal class FileGroupSplitClient : VimFileBase() {

  override fun findFile(filename: String, context: ExecutionContext): String? {
    return rpc { findFile(filename, null) }
  }

  override fun openFile(filename: String, context: ExecutionContext, focusEditor: Boolean): Boolean {
    return rpc { openFile(filename, null, focusEditor) }
  }

  override fun closeFile(editor: VimEditor, context: ExecutionContext) {
    // Closing a window/split is a local UI operation — the window state lives on the thin client.
    // Same pattern as WindowGroup.closeCurrentWindow() which also uses FileEditorManagerEx directly.
    val project = PlatformDataKeys.PROJECT.getData(context.context as DataContext) ?: return
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val window = fileEditorManager.currentWindow
    val virtualFile = fileEditorManager.currentFile

    if (virtualFile != null && window != null) {
      window.closeFile(virtualFile)
      window.requestFocus(true)
    }
  }

  override fun closeFile(number: Int, context: ExecutionContext) {
    rpc { closeFile(number, null) }
  }

  override fun saveFile(editor: VimEditor, context: ExecutionContext) {
    val filePath = editor.getVirtualFile()?.path
    val saveAll = injector.globalIjOptions().ideawrite.contains(IjOptionConstants.ideawrite_all)
    rpc { saveFile(null, filePath, saveAll) }
  }

  override fun saveFiles(editor: VimEditor, context: ExecutionContext) {
    rpc { saveFile(null, null, true) }
  }

  override fun selectFile(count: Int, context: ExecutionContext): Boolean {
    return rpc { selectFile(count, null) }
  }

  override fun selectNextFile(count: Int, context: ExecutionContext) {
    rpc { selectNextFile(count, null) }
  }

  override fun selectPreviousTab(context: ExecutionContext) {
    val success = rpc { selectPreviousTab(null) }
    if (!success) {
      VimPlugin.indicateError()
    }
  }

  override fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean) {
    val filePath = vimEditor.getVirtualFile()?.path
    val message = rpc { buildFileInfoMessage(null, filePath, fullPath) }
    if (message != null) {
      VimPlugin.showMessage(message)
    }
  }

  override fun selectEditor(projectId: String, documentPath: String, protocol: String?): VimEditor? {
    // Forward to backend to open/select the file
    val success = rpc { selectEditor(projectId, documentPath, protocol) }
    if (!success) return null

    // Get local editor reference (platform syncs editor state from backend)
    val fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol) ?: return null
    val virtualFile = fileSystem.findFileByPath(documentPath) ?: return null
    val project = ProjectManager.getInstance().openProjects
      .firstOrNull { getProjectId(it) == projectId } ?: return null
    val fMgr = FileEditorManager.getInstance(project)
    val feditors = fMgr.openFile(virtualFile, true)
    if (feditors.isNotEmpty() && feditors[0] is TextEditor) {
      val editor = (feditors[0] as TextEditor).editor
      if (!editor.isDisposed) return editor.vim
    }
    return null
  }

  override fun getProjectId(project: Any): String {
    require(project is Project)
    return project.name + "-" + project.locationHash
  }

  private fun <T> rpc(block: suspend FileRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { FileRemoteApi.getInstance().block() }
  }
}
