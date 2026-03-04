/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking

/**
 * Split-mode (thin client) [FileBackendService] implementation.
 *
 * Every operation is forwarded to the backend via [FileRemoteApi] RPC.
 * The only local state is the [cachedProjectId] which avoids an RPC
 * round-trip on every [getProjectId]/[getProjectIdForProject] call.
 *
 * Note: `projectBasePath` is always `null` in RPC calls because the thin
 * client's sandbox path differs from the backend's real project path.
 * The backend resolves `null` to its first open project.
 */
internal class FileBackendServiceSplitClient : FileBackendService {

  override fun findFile(filename: String, projectId: String?): String? {
    return rpc { findFile(filename, null) }
  }

  override fun openFile(filename: String, projectId: String?, focusEditor: Boolean): String? {
    return rpc { openFile(filename, null, focusEditor) }
  }

  override fun closeFileByNumber(number: Int, projectId: String?) {
    rpc { closeFile(number, null) }
  }

  override fun saveFile(projectId: String?, filePath: String?, saveAll: Boolean) {
    rpc { saveFile(null, filePath, saveAll) }
  }

  override fun buildFileInfoMessage(projectId: String?, filePath: String?, fullPath: Boolean): String? {
    return rpc { buildFileInfoMessage(null, filePath, fullPath) }
  }

  override fun selectEditor(projectId: String, documentPath: String, protocol: String): Boolean {
    return rpc { selectEditor(projectId, documentPath, protocol) }
  }

  override fun getProjectId(): String {
    return cachedProjectId
  }

  override fun getProjectIdForProject(project: Any): String {
    return cachedProjectId
  }

  private val cachedProjectId by lazy { rpc { getProjectId() } }

  private fun <T> rpc(block: suspend FileRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { FileRemoteApi.getInstance().block() }
  }
}
