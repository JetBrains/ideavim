/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorsSplitters
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.group.findEditorByFilePath
import com.maddyhome.idea.vim.group.findProjectById
import com.maddyhome.idea.vim.group.findVirtualFile
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RPC handler for [FileRemoteApi].
 * Instantiated by [FileRemoteApiProvider] during extension registration.
 * Delegates to [FileBackendServiceImpl] for backend-dependent operations.
 *
 * RPC calls arrive on a background thread, but backend APIs use Swing/EDT,
 * so every delegation switches to [Dispatchers.EDT].
 *
 * Methods that exist on the [FileBackendService] interface delegate directly.
 * Methods for local-only UI operations (selectFile, selectNextFile, closeCurrentFile)
 * are inlined here since they are not part of [FileBackendService].
 */
internal class FileRemoteApiImpl : FileRemoteApi {

  private val fileBackend: FileBackendServiceImpl
    get() = service<FileBackendService>() as FileBackendServiceImpl

  override suspend fun findFile(filename: String, projectBasePath: String?): String? = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext null
    fileBackend.findFile(filename, project)?.path
  }

  override suspend fun openFile(filename: String, projectBasePath: String?, focusEditor: Boolean): String? =
    withContext(Dispatchers.EDT) {
      val project = findProject(projectBasePath) ?: return@withContext "No project found"
      val found = fileBackend.findFile(filename, project)
      if (found != null) {
        val type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(found, project)
        if (type != null) {
          FileEditorManager.getInstance(project).openFile(found, focusEditor)
        }
        null // success
      } else {
        EngineMessageHelper.message("message.open.file.not.found", filename)
      }
    }

  override suspend fun closeCurrentFile(projectBasePath: String?, filePath: String?) = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext
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
      val vf = filePath?.let { findVirtualFile(it) }
      if (vf != null) {
        fileEditorManager.closeFile(vf)
      }
    }
  }

  override suspend fun closeFile(number: Int, projectBasePath: String?) = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val window = fileEditorManager.currentWindow
    val editors = fileEditorManager.openFiles
    if (window != null) {
      if (number >= 0 && number < editors.size) {
        fileEditorManager.closeFile(editors[number], window)
      }
    }
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
    }
  }

  override suspend fun saveFile(projectBasePath: String?, filePath: String?, saveAll: Boolean) =
    withContext(Dispatchers.EDT) {
      val project = findProject(projectBasePath) ?: return@withContext
      val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return@withContext
      fileBackend.saveFile(editor, saveAll)
    }

  override suspend fun selectFile(count: Int, projectBasePath: String?): Boolean = withContext(Dispatchers.EDT) {
    var idx = count
    val project = findProject(projectBasePath) ?: return@withContext false
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    if (idx == 99) {
      idx = editors.size - 1
    }
    if (idx < 0 || idx >= editors.size) {
      return@withContext false
    }
    fem.openFile(editors[idx], true)
    true
  }

  override suspend fun selectNextFile(count: Int, projectBasePath: String?) = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    val current = fem.selectedFiles.getOrNull(0) ?: return@withContext
    for (i in editors.indices) {
      if (editors[i] == current) {
        val pos = (i + (count % editors.size) + editors.size) % editors.size
        fem.openFile(editors[pos], true)
      }
    }
  }

  override suspend fun buildFileInfoMessage(projectBasePath: String?, filePath: String?, fullPath: Boolean): String? =
    withContext(Dispatchers.EDT) {
      val project = findProject(projectBasePath) ?: return@withContext null
      val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return@withContext null
      fileBackend.buildFileInfoMessage(editor, fullPath)
    }

  override suspend fun selectEditor(projectId: String, documentPath: String, protocol: String): Boolean =
    withContext(Dispatchers.EDT) {
      val virtualFile = findVirtualFile(documentPath, protocol) ?: return@withContext false
      val project = findProjectById(projectId) ?: return@withContext false
      val editor = fileBackend.selectEditor(project, virtualFile)
      editor != null
    }

  override suspend fun getProjectId(): String = withContext(Dispatchers.EDT) {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()
      ?: error("No open projects on backend")
    fileBackend.getProjectId(project)
  }

  private fun findProject(projectBasePath: String?): Project? {
    val projects = ProjectManager.getInstance().openProjects
    if (projectBasePath == null) return projects.firstOrNull()
    return projects.firstOrNull { it.basePath == projectBasePath }
  }
}
