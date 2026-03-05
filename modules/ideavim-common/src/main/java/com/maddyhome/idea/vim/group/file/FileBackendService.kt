/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.file

import com.intellij.openapi.components.service

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
 * All parameters are serializable (String, Int, Boolean) so the interface can be
 * implemented as an RPC proxy for split mode.
 */
interface FileBackendService {

  fun findFile(filename: String, projectId: String?): String?

  /**
   * Opens a file on the backend.
   * @return null on success, or an error message to display on the frontend
   */
  fun openFile(filename: String, projectId: String?, focusEditor: Boolean = true): String?

  fun closeFileByNumber(number: Int, projectId: String?)

  /**
   * Saves file(s) based on the [saveAll] flag.
   * The option is already resolved by the frontend caller.
   */
  fun saveFile(projectId: String?, filePath: String?, saveAll: Boolean)

  /**
   * Builds the `:file` / Ctrl-G message string for the given file.
   * @return the message to display, or null if no info is available
   */
  fun buildFileInfoMessage(projectId: String?, filePath: String?, fullPath: Boolean): String?

  /**
   * Focuses or opens a file by path.
   * @return true if the file was successfully opened/focused
   */
  fun selectEditor(projectId: String, documentPath: String, protocol: String): Boolean

  /**
   * Returns the project ID for the first open project.
   */
  fun getProjectId(): String

  /**
   * Returns the project ID for the given project.
   * In split mode, returns the cached backend project ID (ignoring the project param).
   */
  fun getProjectIdForProject(project: Any): String

  companion object {
    @JvmStatic
    fun getInstance(): FileBackendService = service<FileBackendService>()
  }
}
