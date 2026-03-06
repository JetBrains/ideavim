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

/**
 * Backend service for file operations that require IntelliJ Platform APIs
 * (VFS, PSI, FileEditorManager, ProjectRootManager, etc.).
 *
 * In **monolith mode**, [FileBackendServiceImpl] provides direct implementations.
 * In **split mode**, [FileBackendServiceSplitClient] forwards calls via [FileRemoteApi] RPC.
 *
 * The frontend [IjFileGroup] (the sole [VimFile][com.maddyhome.idea.vim.api.VimFile] implementation)
 * delegates backend-dependent operations to this service while keeping local UI operations
 * (closeFile by editor, selectFile, selectNextFile) on the frontend.
 *
 * Uses platform RPC IDs ([ProjectId], [EditorId]) for cross-process identity transfer.
 */
interface FileBackendService {

  fun findFile(filename: String, projectId: ProjectId?): String?

  /**
   * Opens a file on the backend.
   * @return null on success, or an error message to display on the frontend
   */
  fun openFile(filename: String, projectId: ProjectId?, focusEditor: Boolean = true): String?

  fun closeFileByNumber(number: Int, projectId: ProjectId?)

  /**
   * Saves file(s) based on the [saveAll] flag.
   * The option is already resolved by the frontend caller.
   * The [editorId] identifies the editor whose document should be saved.
   */
  fun saveFile(editorId: EditorId, saveAll: Boolean)

  /**
   * Builds the `:file` / Ctrl-G message string for the given editor.
   * @return the message to display, or null if no info is available
   */
  fun buildFileInfoMessage(editorId: EditorId, fullPath: Boolean): String?

  /**
   * Focuses or opens a file by path.
   * @return true if the file was successfully opened/focused
   */
  fun selectEditor(projectId: ProjectId, documentPath: String, protocol: String): Boolean

  companion object {
    @JvmStatic
    fun getInstance(): FileBackendService = service<FileBackendService>()
  }
}
