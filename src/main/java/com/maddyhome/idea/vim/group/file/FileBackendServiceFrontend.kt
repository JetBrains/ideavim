/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.platform.project.ProjectId
import com.maddyhome.idea.vim.group.rpc

/**
 * Unified frontend [FileBackendService] that always uses RPC.
 *
 * Works in both monolith and split mode because backend RPC handlers
 * use `isDispatchThread` to avoid `withContext(EDT)` when already on EDT.
 */
internal class FileBackendServiceFrontend : FileBackendService {

  override fun findFile(filename: String, projectId: ProjectId?): String? {
    return rpc { FileRemoteApi.getInstance().findFile(filename, projectId) }
  }

  override fun openFile(filename: String, projectId: ProjectId?, focusEditor: Boolean): String? {
    return rpc { FileRemoteApi.getInstance().openFile(filename, projectId, focusEditor) }
  }

  override fun closeFileByNumber(number: Int, projectId: ProjectId?) {
    rpc { FileRemoteApi.getInstance().closeFile(number, projectId) }
  }

  override fun saveFile(editorId: EditorId, saveAll: Boolean) {
    rpc { FileRemoteApi.getInstance().saveFile(editorId, saveAll) }
  }

  override fun buildFileInfoMessage(editorId: EditorId, fullPath: Boolean): String? {
    return rpc { FileRemoteApi.getInstance().buildFileInfoMessage(editorId, fullPath) }
  }

  override fun selectEditor(projectId: ProjectId, documentPath: String, protocol: String): Boolean {
    return rpc { FileRemoteApi.getInstance().selectEditor(projectId, documentPath, protocol) }
  }

  companion object {
    @JvmStatic
    fun getInstance(): FileBackendServiceFrontend = service<FileBackendService>() as FileBackendServiceFrontend
  }
}
