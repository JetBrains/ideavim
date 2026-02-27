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
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.mark.IntellijMark
import com.maddyhome.idea.vim.mark.VimMark
import com.maddyhome.idea.vim.newapi.globalIjOptions

/**
 * RPC handler for [MarkRemoteApi].
 * Instantiated by [MarkRemoteApiProvider] during extension registration.
 * Delegates to [VimMarkServiceImpl] lazily — the service lookup happens when the
 * RPC call arrives, not when the provider is registered.
 */
internal class MarkRemoteApiImpl : MarkRemoteApi {
  override suspend fun getMark(projectBasePath: String?, char: Char): MarkInfo? {
    val markService = service<VimMarkService>()
    val mark = markService.getGlobalMark(char) ?: return null
    return MarkInfo(mark.key, mark.line, mark.col, mark.filepath, mark.protocol)
  }

  override suspend fun setMark(
    projectBasePath: String?,
    char: Char,
    filePath: String,
    line: Int,
    col: Int,
    protocol: String,
  ): Boolean {
    val markService = service<VimMarkService>()

    // When ideamarks is enabled, create an IDE bookmark on the backend
    if (injector.globalIjOptions().ideamarks) {
      val editor = findEditorForFile(filePath, protocol)
      if (editor != null) {
        val systemMark = SystemMarks.createOrGetSystemMark(char, line, editor)
        if (systemMark != null) {
          val mark = IntellijMark(systemMark, col, editor.project)
          return markService.setGlobalMark(mark)
        }
      }
    }

    // Fallback: ideamarks disabled or no editor open on backend
    val mark = VimMark(char, line, col, filePath, protocol)
    return markService.setGlobalMark(mark)
  }

  override suspend fun removeMark(projectBasePath: String?, char: Char) {
    val markService = service<VimMarkService>()
    markService.removeGlobalMark(char)
  }

  override suspend fun getMarks(projectBasePath: String?): List<MarkInfo> {
    val markService = service<VimMarkService>()
    return markService.getAllGlobalMarks().map { mark ->
      MarkInfo(mark.key, mark.line, mark.col, mark.filepath, mark.protocol)
    }
  }

  private fun findEditorForFile(filePath: String, protocol: String): Editor? {
    // On the backend, files are always local regardless of the frontend's protocol
    val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
    for (project in ProjectManager.getInstance().openProjects) {
      val editor = FileEditorManager.getInstance(project)
        .getAllEditors(virtualFile)
        .filterIsInstance<TextEditor>()
        .firstOrNull()
        ?.editor
      if (editor != null) return editor
    }
    return null
  }
}
