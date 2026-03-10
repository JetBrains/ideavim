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
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.impl.editorId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorsSplitters
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.intellij.platform.project.projectId
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFileBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.file.FileRemoteApi
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim

/**
 * Frontend [VimFile][com.maddyhome.idea.vim.api.VimFile] implementation — the **sole** VimFile service.
 *
 * Extends [VimFileBase] for pure-engine operations (`displayHexInfo`, `displayLocationInfo`).
 *
 * Backend-dependent operations (file finding, opening, saving, file-info messages) are
 * delegated via [FileRemoteApi] RPC. Works in both monolith and split mode.
 *
 * Local UI operations that only affect window/tab state on the frontend
 * (closeFile by editor) remain in this class.
 *
 * Options (e.g. `ideawrite`) are read here on the frontend, never on the backend.
 */
class IjFileGroup : VimFileBase() {

  override fun openFile(filename: String, context: ExecutionContext, focusEditor: Boolean): String? {
    logger.debug { "openFile($filename)" }
    return rpc { FileRemoteApi.getInstance().openFile(filename, extractProjectId(context), focusEditor) }
  }

  override fun findFile(filename: String, context: ExecutionContext): String? {
    return rpc { FileRemoteApi.getInstance().findFile(filename, extractProjectId(context)) }
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
    rpc { FileRemoteApi.getInstance().closeFile(number, extractProjectId(context)) }
  }

  override fun saveFile(editor: VimEditor, context: ExecutionContext) {
    val saveAll = injector.globalIjOptions().ideawrite.contains(IjOptionConstants.ideawrite_all)
    val editorId = (editor as IjVimEditor).editor.editorId()
    rpc { FileRemoteApi.getInstance().saveFile(editorId, saveAll) }
  }

  override fun saveFiles(editor: VimEditor, context: ExecutionContext) {
    val editorId = (editor as IjVimEditor).editor.editorId()
    rpc { FileRemoteApi.getInstance().saveFile(editorId, true) }
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
    val project = PlatformDataKeys.PROJECT.getData(context.context as DataContext) ?: return false
    val vf = LastTabService.getInstance(project).lastTab
    if (vf != null && vf.isValid) {
      FileEditorManager.getInstance(project).openFile(vf, true)
      return true
    }
    return false
  }

  override fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean): String? {
    val editorId = (vimEditor as IjVimEditor).editor.editorId()
    return rpc { FileRemoteApi.getInstance().buildFileInfoMessage(editorId, fullPath) }
  }

  override fun selectEditor(projectId: String, documentPath: String, protocol: String): VimEditor? {
    val platformProjectId = try {
      ProjectId.deserializeFromString(projectId)
    } catch (_: Exception) {
      return null
    }
    val success = rpc { FileRemoteApi.getInstance().selectEditor(platformProjectId, documentPath, protocol) }
    if (!success) return null

    // The backend opened/focused the file. Find the editor it opened.
    val project = platformProjectId.findProjectOrNull()
      ?: ProjectManager.getInstance().openProjects.firstOrNull()
      ?: return null

    val editor = FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>()
      .firstOrNull { it.file.path == documentPath && !it.editor.isDisposed }
      ?.editor
    return if (editor != null) editor.vim else null
  }

  override fun getProjectId(project: Any): String {
    require(project is Project)
    return project.projectId().serializeToString()
  }

  private fun extractProjectId(context: ExecutionContext): ProjectId? {
    val project = PlatformDataKeys.PROJECT.getData(context.context as DataContext) ?: return null
    return project.projectId()
  }

  companion object {
    private val logger = Logger.getInstance(IjFileGroup::class.java.name)
  }
}
