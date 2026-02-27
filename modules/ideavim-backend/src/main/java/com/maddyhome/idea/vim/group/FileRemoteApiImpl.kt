/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.VimFile
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.vim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RPC handler for [FileRemoteApi].
 * Instantiated by [FileRemoteApiProvider] during extension registration.
 * Delegates to [FileGroup] (the backend [VimFile] service) for all operations.
 *
 * RPC calls arrive on a background thread, but [FileGroup] uses Swing/EDT APIs
 * (FileEditorManager, etc.), so every delegation switches to [Dispatchers.EDT].
 *
 * In split mode the backend has no focused window/editor — the UI lives on the
 * thin client. Methods that need the "current" editor receive [filePath] from
 * the thin client and locate the editor via [findEditorByFilePath].
 */
internal class FileRemoteApiImpl : FileRemoteApi {

  private val fileGroup: FileGroup get() = service<VimFile>() as FileGroup

  override suspend fun findFile(filename: String, projectBasePath: String?): String? = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext null
    fileGroup.findFile(filename, project)?.path
  }

  override suspend fun openFile(filename: String, projectBasePath: String?, focusEditor: Boolean): Boolean =
    withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext false
    val context = buildContext(project, null)
      fileGroup.openFile(filename, context, focusEditor)
  }

  override suspend fun closeCurrentFile(projectBasePath: String?, filePath: String?) = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext
    val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return@withContext
    val vimEditor = editor.vim
    val context = buildContext(project, editor)
    fileGroup.closeFile(vimEditor, context)
  }

  override suspend fun closeFile(number: Int, projectBasePath: String?) = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext
    val context = buildContext(project, null)
    fileGroup.closeFile(number, context)
  }

  override suspend fun saveFile(projectBasePath: String?, filePath: String?, saveAll: Boolean) =
    withContext(Dispatchers.EDT) {
      val project = findProject(projectBasePath) ?: return@withContext
      val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return@withContext
      val vimEditor = editor.vim
      val context = buildContext(project, editor)
      if (saveAll) {
        fileGroup.saveFiles(vimEditor, context)
      } else {
        fileGroup.saveFile(vimEditor, context)
      }
    }

  override suspend fun selectFile(count: Int, projectBasePath: String?): Boolean = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext false
    val context = buildContext(project, null)
    fileGroup.selectFile(count, context)
  }

  override suspend fun selectNextFile(count: Int, projectBasePath: String?) = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext
    val context = buildContext(project, null)
    fileGroup.selectNextFile(count, context)
  }

  override suspend fun selectPreviousTab(projectBasePath: String?): Boolean = withContext(Dispatchers.EDT) {
    val project = findProject(projectBasePath) ?: return@withContext false
    val vf = LastTabService.getInstance(project).lastTab
    if (vf != null && vf.isValid) {
      FileEditorManager.getInstance(project).openFile(vf, true)
      true
    } else {
      false
    }
  }

  override suspend fun buildFileInfoMessage(projectBasePath: String?, filePath: String?, fullPath: Boolean): String? =
    withContext(Dispatchers.EDT) {
      val project = findProject(projectBasePath) ?: return@withContext null
      val editor = filePath?.let { findEditorByFilePath(project, it) } ?: return@withContext null
      fileGroup.buildFileInfoMessage(editor.vim, fullPath)
    }

  override suspend fun selectEditor(projectId: String, documentPath: String, protocol: String?): Boolean =
    withContext(Dispatchers.EDT) {
      val vimEditor = fileGroup.selectEditor(projectId, documentPath, protocol)
      vimEditor != null
    }

  private fun findProject(projectBasePath: String?): Project? {
    val projects = ProjectManager.getInstance().openProjects
    if (projectBasePath == null) return projects.firstOrNull()
    return projects.firstOrNull { it.basePath == projectBasePath }
  }

  private fun findVirtualFile(filePath: String): VirtualFile? {
    return LocalFileSystem.getInstance().findFileByPath(filePath)
  }

  private fun findEditorByFilePath(project: Project, filePath: String): Editor? {
    val vf = findVirtualFile(filePath) ?: return null
    return FileEditorManager.getInstance(project).getAllEditors(vf)
      .filterIsInstance<TextEditor>()
      .firstOrNull()
      ?.editor
  }

  private fun buildContext(project: Project, editor: Editor?): IjEditorExecutionContext {
    val dataContext = SimpleDataContext.builder()
      .add(PlatformDataKeys.PROJECT, project)
      .apply { if (editor != null) add(CommonDataKeys.EDITOR, editor) }
      .build()
    return IjEditorExecutionContext(dataContext)
  }
}
