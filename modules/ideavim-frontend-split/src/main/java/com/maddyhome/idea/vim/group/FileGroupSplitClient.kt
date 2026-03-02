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
import java.nio.file.Path

/**
 * Thin-client [VimFile][com.maddyhome.idea.vim.api.VimFile] for split (Remote Development) mode.
 *
 * Extends [VimFileBase] for pure-engine operations (`displayHexInfo`, `displayLocationInfo`).
 * All other operations are forwarded to the backend via [FileRemoteApi] RPC.
 * The only local work is extracting serializable parameters and showing messages/errors.
 *
 * Note: `projectBasePath` is always `null` because the thin client's project path
 * (sandbox path) differs from the backend's real project path. The backend resolves
 * `null` to its first open project.
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
    // Buffer selection is a local UI operation — the tab order lives on the thin client.
    var idx = count
    val project = PlatformDataKeys.PROJECT.getData(context.context as DataContext) ?: return false
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    if (idx == 99) {
      idx = editors.size - 1
    }
    if (idx < 0 || idx >= editors.size) {
      return false
    }
    fem.openFile(editors[idx], true)
    return true
  }

  override fun selectNextFile(count: Int, context: ExecutionContext) {
    // Buffer navigation is a local UI operation — the tab order lives on the thin client.
    val project = PlatformDataKeys.PROJECT.getData(context.context as DataContext) ?: return
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    val current = fem.selectedFiles.firstOrNull() ?: return
    for (i in editors.indices) {
      if (editors[i] == current) {
        val pos = (i + (count % editors.size) + editors.size) % editors.size
        fem.openFile(editors[pos], true)
      }
    }
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

  override fun selectEditor(projectId: String, documentPath: String, protocol: String): VimEditor? {
    // Ask the backend to open/focus the file (ensures it's available).
    val success = rpc { selectEditor(projectId, documentPath, protocol) }
    if (!success) return null

    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return null

    val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(documentPath))
      ?: return null
    val editors = FileEditorManager.getInstance(project).openFile(virtualFile, true)
    val textEditor = editors.filterIsInstance<TextEditor>().firstOrNull()?.editor
    if (textEditor != null && !textEditor.isDisposed) return textEditor.vim
    return null
  }

  private val cachedProjectId by lazy { rpc { getProjectId() } }

  override fun getProjectId(project: Any): String {
    return cachedProjectId
  }

  private fun <T> rpc(block: suspend FileRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { FileRemoteApi.getInstance().block() }
  }
}
