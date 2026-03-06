/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RPC handler for [BookmarkRemoteApi].
 * Delegates to [BookmarkBackendService] for the actual bookmark operations.
 *
 * RPC calls arrive on a background thread, but bookmark APIs use Swing/EDT,
 * so every delegation switches to [Dispatchers.EDT].
 */
internal class BookmarkRemoteApiImpl : BookmarkRemoteApi {

  private val bookmarkBackend: BookmarkBackendService
    get() = service<BookmarkBackendService>()

  override suspend fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    virtualFileId: VirtualFileId,
    projectId: ProjectId?,
  ): BookmarkInfo? = withContext(Dispatchers.EDT) {
    bookmarkBackend.createOrGetSystemMark(char, line, col, virtualFileId, projectId)
  }

  override suspend fun removeBookmark(char: Char) = withContext(Dispatchers.EDT) {
    bookmarkBackend.removeBookmark(char)
  }

  override suspend fun getBookmarkForMark(char: Char): BookmarkInfo? = withContext(Dispatchers.EDT) {
    bookmarkBackend.getBookmarkForMark(char)
  }

  override suspend fun getAllBookmarks(): List<BookmarkInfo> = withContext(Dispatchers.EDT) {
    bookmarkBackend.getAllBookmarks()
  }
}
