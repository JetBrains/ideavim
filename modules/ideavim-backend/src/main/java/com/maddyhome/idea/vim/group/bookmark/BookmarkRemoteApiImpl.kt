/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.group.BookmarkBackendService
import com.maddyhome.idea.vim.group.BookmarkInfo
import com.maddyhome.idea.vim.group.BookmarkRemoteApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RPC handler for [BookmarkRemoteApi].
 * Delegates to [BookmarkBackendServiceImpl] for the actual bookmark operations.
 *
 * RPC calls arrive on a background thread, but bookmark APIs use Swing/EDT,
 * so every delegation switches to [Dispatchers.EDT].
 */
internal class BookmarkRemoteApiImpl : BookmarkRemoteApi {

  private val bookmarkBackend: BookmarkBackendServiceImpl
    get() = service<BookmarkBackendService>() as BookmarkBackendServiceImpl

  override suspend fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    filePath: String,
    projectId: String?,
    protocol: String?,
  ): BookmarkInfo? = withContext(Dispatchers.EDT) {
    bookmarkBackend.createOrGetSystemMark(char, line, col, filePath, projectId, protocol)
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
