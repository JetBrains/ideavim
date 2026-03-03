/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for all [VimFile][com.maddyhome.idea.vim.api.VimFile] operations.
 *
 * Called from [FileGroupSplitClient] in thin-client mode to forward every file
 * operation to the backend where [FileGroup] manages VFS, PSI, editors, and documents.
 * The split client is a thin proxy — it extracts serializable parameters and forwards via RPC.
 *
 * Many methods accept a [filePath] parameter because the backend in split mode has no
 * focused window/editor — the UI lives on the thin client. The thin client passes the
 * current file path so the backend can locate the correct editor and virtual file.
 */
@Rpc
@ApiStatus.Internal
interface FileRemoteApi : RemoteApi<Unit> {

  suspend fun findFile(filename: String, projectBasePath: String?): String?

  /**
   * Opens a file on the backend.
   * @return null on success, or an error message to display on the frontend
   */
  suspend fun openFile(filename: String, projectBasePath: String?, focusEditor: Boolean = true): String?
  suspend fun closeCurrentFile(projectBasePath: String?, filePath: String?)
  suspend fun closeFile(number: Int, projectBasePath: String?)
  suspend fun saveFile(projectBasePath: String?, filePath: String?, saveAll: Boolean)
  suspend fun selectFile(count: Int, projectBasePath: String?): Boolean
  suspend fun selectNextFile(count: Int, projectBasePath: String?)
  suspend fun selectPreviousTab(projectBasePath: String?): Boolean
  suspend fun buildFileInfoMessage(projectBasePath: String?, filePath: String?, fullPath: Boolean): String?
  suspend fun selectEditor(projectId: String, documentPath: String, protocol: String): Boolean
  suspend fun getProjectId(): String

  companion object {
    @JvmStatic
    suspend fun getInstance(): FileRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<FileRemoteApi>())
    }
  }
}
