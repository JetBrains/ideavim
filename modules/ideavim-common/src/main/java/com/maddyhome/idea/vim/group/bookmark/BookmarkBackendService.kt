/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId

/**
 * Backend service for IDE bookmark operations.
 *
 * In **monolith mode**, [BookmarkBackendServiceImpl] provides direct implementations
 * using [com.intellij.ide.bookmark.BookmarksManager].
 * In **split mode**, [BookmarkBackendServiceSplitClient] forwards calls via [BookmarkRemoteApi] RPC.
 *
 * The frontend [VimMarkServiceImpl] delegates bookmark-dependent operations to this service
 * while keeping all mark logic (ideamarks checks, VimMark creation) on the frontend.
 *
 * Project identification uses platform [ProjectId] which is RPC-serializable and
 * resolves correctly across frontend/backend processes.
 */
interface BookmarkBackendService {

  /**
   * Creates or retrieves an IDE bookmark on the backend.
   *
   * @param char the mark character (e.g. 'A')
   * @param line the 0-based line number
   * @param filePath the file path where the bookmark should be placed
   * @param projectId platform project ID for resolving the correct project on backend
   * @return bookmark info if created/found, null if bookmark creation failed
   */
  fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    filePath: String,
    projectId: ProjectId?,
    protocol: String? = null,
  ): BookmarkInfo?

  /**
   * Removes an IDE bookmark.
   *
   * @param char the mark character (e.g. 'A')
   */
  fun removeBookmark(char: Char)

  /**
   * Retrieves the current IDE bookmark for a given mark character.
   *
   * @param char the mark character (e.g. 'A')
   * @return bookmark info if a bookmark with this mnemonic exists, null otherwise
   */
  fun getBookmarkForMark(char: Char): BookmarkInfo?

  /**
   * Retrieves all mnemonic IDE bookmarks (A-Z, 0-9).
   *
   * @return list of all existing mnemonic bookmarks
   */
  fun getAllBookmarks(): List<BookmarkInfo>

  companion object {
    @JvmStatic
    fun getInstance(): BookmarkBackendService = service<BookmarkBackendService>()
  }
}
