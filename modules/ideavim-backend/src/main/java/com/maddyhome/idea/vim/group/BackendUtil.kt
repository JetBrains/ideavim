/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
/**
 * Resolves a [Project] by its projectId.
 * Matches by [FileBackendServiceImpl.getProjectId] first, falls back to the first open project.
 *
 * Note: Uses [service] instead of `injector.file` because `injector` is not
 * initialized on the backend in split mode.
 */
internal fun findProjectById(projectId: String?): Project? {
  val projects = ProjectManager.getInstance().openProjects
  if (projectId == null) return projects.firstOrNull()
  val fileBackend = service<FileBackendService>()
  return projects.firstOrNull { fileBackend.getProjectIdForProject(it) == projectId }
    ?: projects.firstOrNull()
}

/**
 * Finds a [VirtualFile] by path, trying local filesystem first, then jar for library sources.
 * When [protocol] is provided, tries that filesystem first before falling back.
 */
internal fun findVirtualFile(filePath: String, protocol: String? = null): VirtualFile? {
  if (protocol != null) {
    VirtualFileManager.getInstance().getFileSystem(protocol)?.findFileByPath(filePath)?.let { return it }
  }
  return LocalFileSystem.getInstance().findFileByPath(filePath)
    ?: VirtualFileManager.getInstance().getFileSystem("jar")?.findFileByPath(filePath)
}

/**
 * Finds an open [Editor] for the given file path in the specified project.
 */
internal fun findEditorByFilePath(project: Project, filePath: String): Editor? {
  val vf = findVirtualFile(filePath) ?: return null
  return FileEditorManager.getInstance(project).getAllEditors(vf)
    .filterIsInstance<TextEditor>()
    .firstOrNull()
    ?.editor
}
