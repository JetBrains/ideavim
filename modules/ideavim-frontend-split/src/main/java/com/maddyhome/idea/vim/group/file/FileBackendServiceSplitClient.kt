/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId
import com.maddyhome.idea.vim.group.CoroutineScopeProvider
import kotlinx.coroutines.runBlocking

/**
 * Split-mode (thin client) [FileBackendService] implementation.
 *
 * Every operation is forwarded to the backend via [FileRemoteApi] RPC.
 * Project identification uses platform [ProjectId] which resolves correctly
 * across frontend/backend processes — no caching or custom ID generation needed.
 */
internal class FileBackendServiceSplitClient : FileBackendService {

  override fun findFile(filename: String, projectId: ProjectId?): String? {
    return rpc { findFile(filename, projectId) }
  }

  override fun openFile(filename: String, projectId: ProjectId?, focusEditor: Boolean): String? {
    return rpc { openFile(filename, projectId, focusEditor) }
  }

  override fun closeFileByNumber(number: Int, projectId: ProjectId?) {
    rpc { closeFile(number, projectId) }
  }

  override fun saveFile(projectId: ProjectId?, filePath: String?, saveAll: Boolean) {
    rpc { saveFile(projectId, filePath, saveAll) }
  }

  override fun buildFileInfoMessage(projectId: ProjectId?, filePath: String?, fullPath: Boolean): String? {
    return rpc { buildFileInfoMessage(projectId, filePath, fullPath) }
  }

  override fun selectEditor(projectId: ProjectId, documentPath: String, protocol: String): Boolean {
    return rpc { selectEditor(projectId, documentPath, protocol) }
  }

  private fun <T> rpc(block: suspend FileRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { FileRemoteApi.getInstance().block() }
  }
}
