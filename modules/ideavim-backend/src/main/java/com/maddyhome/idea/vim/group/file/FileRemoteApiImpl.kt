/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.ide.vfs.virtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorsSplitters
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.maddyhome.idea.vim.group.findVirtualFile
import com.maddyhome.idea.vim.group.onEdt
import com.maddyhome.idea.vim.helper.EngineMessageHelper

/**
 * RPC handler for [FileRemoteApi].
 * Delegates to [FileBackendServiceImpl] for backend-dependent operations.
 *
 * Uses [onEdt] to dispatch to EDT only when not already on it:
 * - **Monolith**: RPC resolves locally, handler runs on EDT → skip `withContext(EDT)`
 * - **Split**: RPC arrives on a background thread → `withContext(EDT)` dispatches to backend EDT
 */
internal class FileRemoteApiImpl : FileRemoteApi {

  private val fileBackend: FileBackendServiceImpl
    get() = service()

  override suspend fun findFile(filename: String, projectId: ProjectId?): String? = onEdt {
    val project = projectId?.findProjectOrNull() ?: return@onEdt null
    fileBackend.findFile(filename, project)?.path
  }

  override suspend fun openFile(filename: String, projectId: ProjectId?, focusEditor: Boolean): String? =
    onEdt {
      val project = projectId?.findProjectOrNull() ?: return@onEdt "No project found"
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

  override suspend fun closeCurrentFile(projectId: ProjectId?, virtualFileId: VirtualFileId?) =
    onEdt {
      val project = projectId?.findProjectOrNull() ?: return@onEdt
      val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
      val window = fileEditorManager.currentWindow
      val currentFile = fileEditorManager.currentFile

      if (currentFile != null && window != null) {
        window.closeFile(currentFile)
        window.requestFocus(true)
        if (!ApplicationManager.getApplication().isUnitTestMode) {
          EditorsSplitters.focusDefaultComponentInSplittersIfPresent(project)
        }
      } else {
        val vf = virtualFileId?.virtualFile()
        if (vf != null) {
          fileEditorManager.closeFile(vf)
        }
      }
    }

  override suspend fun closeFile(number: Int, projectId: ProjectId?) = onEdt {
    val project = projectId?.findProjectOrNull() ?: return@onEdt
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

  override suspend fun saveFile(editorId: EditorId, saveAll: Boolean) =
    onEdt {
      val editor = editorId.findEditorOrNull() ?: return@onEdt
      fileBackend.saveFile(editor, saveAll)
    }

  override suspend fun selectFile(count: Int, projectId: ProjectId?): Boolean = onEdt {
    var idx = count
    val project = projectId?.findProjectOrNull() ?: return@onEdt false
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    if (idx == 99) {
      idx = editors.size - 1
    }
    if (idx < 0 || idx >= editors.size) {
      return@onEdt false
    }
    fem.openFile(editors[idx], true)
    true
  }

  override suspend fun selectNextFile(count: Int, projectId: ProjectId?) = onEdt {
    val project = projectId?.findProjectOrNull() ?: return@onEdt
    val fem = FileEditorManager.getInstance(project)
    val editors = fem.openFiles
    val current = fem.selectedFiles.getOrNull(0) ?: return@onEdt
    for (i in editors.indices) {
      if (editors[i] == current) {
        val pos = (i + (count % editors.size) + editors.size) % editors.size
        fem.openFile(editors[pos], true)
      }
    }
  }

  override suspend fun buildFileInfoMessage(editorId: EditorId, fullPath: Boolean): String? =
    onEdt {
      val editor = editorId.findEditorOrNull() ?: return@onEdt null
      val project = editor.project ?: return@onEdt null
      fileBackend.buildFileInfoMessage(editor, project, fullPath)
    }

  override suspend fun selectEditor(projectId: ProjectId, documentPath: String, protocol: String): Boolean =
    onEdt {
      val virtualFile = findVirtualFile(documentPath, protocol) ?: return@onEdt false
      val project = projectId.findProjectOrNull() ?: return@onEdt false
      val editor = fileBackend.selectEditor(project, virtualFile)
      editor != null
    }
}
