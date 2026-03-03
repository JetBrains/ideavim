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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorsSplitters
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFileBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim

/**
 * Frontend [VimFile][com.maddyhome.idea.vim.api.VimFile] implementation — the **sole** VimFile service.
 *
 * Extends [VimFileBase] for pure-engine operations (`displayHexInfo`, `displayLocationInfo`).
 *
 * Backend-dependent operations (file finding, opening, saving, file-info messages) are
 * delegated to [FileBackendService]:
 * - In **monolith mode**, [FileBackendServiceImpl] provides direct IntelliJ API access.
 * - In **split mode**, [FileBackendServiceSplitClient] forwards via [FileRemoteApi] RPC.
 *
 * Local UI operations that only affect window/tab state on the frontend
 * (closeFile by editor, selectFile, selectNextFile) remain in this class.
 *
 * Options (e.g. `ideawrite`) are read here on the frontend, never on the backend.
 */
class IjFileGroup : VimFileBase() {

  private val backend: FileBackendService get() = FileBackendService.getInstance()

  override fun openFile(filename: String, context: ExecutionContext, focusEditor: Boolean): String? {
    if (logger.isDebugEnabled) {
      logger.debug("openFile($filename)")
    }
    return backend.openFile(filename, extractProjectId(context), focusEditor)
  }

  override fun findFile(filename: String, context: ExecutionContext): String? {
    return backend.findFile(filename, extractProjectId(context))
  }

  override fun closeFile(editor: VimEditor, context: ExecutionContext) {
    val project = PlatformDataKeys.PROJECT.getData((context.context as DataContext))
    if (project != null) {
      val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
      val window = fileEditorManager.currentWindow
      val virtualFile = fileEditorManager.currentFile

      if (virtualFile != null && window != null) {
        window.closeFile(virtualFile)
        window.requestFocus(true)
        if (!ApplicationManager.getApplication().isUnitTestMode) {
          EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
        }
      } else {
        val vimVf = editor.getVirtualFile()
        val vf = vimVf?.let {
          VirtualFileManager.getInstance().getFileSystem(it.protocol)?.findFileByPath(it.path)
        }
        if (vf != null) {
          fileEditorManager.closeFile(vf)
        }
      }
    }
  }

  override fun closeFile(number: Int, context: ExecutionContext) {
    backend.closeFileByNumber(number, extractProjectId(context))
  }

  override fun saveFile(editor: VimEditor, context: ExecutionContext) {
    val saveAll = injector.globalIjOptions().ideawrite.contains(IjOptionConstants.ideawrite_all)
    val filePath = editor.getVirtualFile()?.path
    backend.saveFile(extractProjectId(context), filePath, saveAll)
  }

  override fun saveFiles(editor: VimEditor, context: ExecutionContext) {
    val filePath = editor.getVirtualFile()?.path
    backend.saveFile(extractProjectId(context), filePath, true)
  }

  override fun selectFile(count: Int, context: ExecutionContext): Boolean {
    var count = count
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return false
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    if (count == 99) {
      count = editors.size - 1
    }
    if (count < 0 || count >= editors.size) {
      return false
    }

    fem.openFile(editors[count], true)

    return true
  }

  override fun selectNextFile(count: Int, context: ExecutionContext) {
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    val current = fem.selectedFiles[0]
    for (i in editors.indices) {
      if (editors[i] == current) {
        val pos = (i + (count % editors.size) + editors.size) % editors.size

        fem.openFile(editors[pos], true)
      }
    }
  }

  override fun selectPreviousTab(context: ExecutionContext): Boolean {
    return backend.selectPreviousTab(extractProjectId(context))
  }

  override fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean): String? {
    val filePath = vimEditor.getVirtualFile()?.path
    val projectId = vimEditor.projectId
    return backend.buildFileInfoMessage(projectId, filePath, fullPath)
  }

  override fun selectEditor(projectId: String, documentPath: String, protocol: String): VimEditor? {
    val success = backend.selectEditor(projectId, documentPath, protocol)
    if (!success) return null

    // Get the opened VimEditor locally
    val virtualFile = VirtualFileManager.getInstance().getFileSystem(protocol)?.findFileByPath(documentPath)
      ?: VirtualFileManager.getInstance().getFileSystem("file")?.findFileByPath(documentPath)
      ?: VirtualFileManager.getInstance().getFileSystem("jar")?.findFileByPath(documentPath)
      ?: return null

    val project = ProjectManager.getInstance().openProjects
      .firstOrNull { backend.getProjectIdForProject(it) == projectId }
      ?: ProjectManager.getInstance().openProjects.firstOrNull()
      ?: return null

    val feditors = FileEditorManager.getInstance(project).openFile(virtualFile, true)
    val textEditor = feditors.filterIsInstance<TextEditor>().firstOrNull()?.editor
    return if (textEditor != null && !textEditor.isDisposed) textEditor.vim else null
  }

  override fun getProjectId(project: Any): String {
    return backend.getProjectIdForProject(project)
  }

  private fun extractProjectId(context: ExecutionContext): String? {
    val project = PlatformDataKeys.PROJECT.getData(context.context as DataContext) ?: return null
    return backend.getProjectIdForProject(project)
  }

  companion object {
    private val logger = Logger.getInstance(IjFileGroup::class.java.name)
  }
}
