/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.platform.project.ProjectId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for all [VimFile][com.maddyhome.idea.vim.api.VimFile] operations.
 *
 * Called from [FileBackendServiceSplitClient] in thin-client mode to forward every file
 * operation to the backend where [FileBackendServiceImpl] manages VFS, PSI, editors, and documents.
 * The split client is a thin proxy — it extracts serializable parameters and forwards via RPC.
 *
 * Uses platform RPC IDs ([ProjectId], [EditorId], [VirtualFileId]) for cross-process
 * identity transfer instead of string-based lookups.
 */
@Rpc
@ApiStatus.Internal
interface FileRemoteApi : RemoteApi<Unit> {

  suspend fun findFile(filename: String, projectId: ProjectId?): String?

  /**
   * Opens a file on the backend.
   * @return null on success, or an error message to display on the frontend
   */
  suspend fun openFile(filename: String, projectId: ProjectId?, focusEditor: Boolean = true): String?
  suspend fun closeCurrentFile(projectId: ProjectId?, virtualFileId: VirtualFileId?)
  suspend fun closeFile(number: Int, projectId: ProjectId?)
  suspend fun saveFile(editorId: EditorId, saveAll: Boolean)
  suspend fun selectFile(count: Int, projectId: ProjectId?): Boolean
  suspend fun selectNextFile(count: Int, projectId: ProjectId?)
  suspend fun buildFileInfoMessage(editorId: EditorId, fullPath: Boolean): String?
  suspend fun selectEditor(projectId: ProjectId, documentPath: String, protocol: String): Boolean

  companion object {
    @JvmStatic
    suspend fun getInstance(): FileRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<FileRemoteApi>())
    }
  }
}
