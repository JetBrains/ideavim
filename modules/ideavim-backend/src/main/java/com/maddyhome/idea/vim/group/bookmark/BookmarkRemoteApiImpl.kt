/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.ide.vfs.VirtualFileId
import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [BookmarkRemoteApi].
 * Delegates to [BookmarkBackendServiceImpl] for the actual bookmark operations.
 *
 * Uses [onEdt] to dispatch to EDT only when not already on it:
 * - **Monolith**: RPC resolves locally, handler runs on EDT → skip `withContext(EDT)`
 * - **Split**: RPC arrives on a background thread → `withContext(EDT)` dispatches to backend EDT
 */
internal class BookmarkRemoteApiImpl : BookmarkRemoteApi {

  private val bookmarkBackend: BookmarkBackendServiceImpl
    get() = service()

  override suspend fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    virtualFileId: VirtualFileId,
    projectId: ProjectId?,
  ): BookmarkInfo? = onEdt {
    bookmarkBackend.createOrGetSystemMark(char, line, col, virtualFileId, projectId)
  }

  override suspend fun removeBookmark(char: Char) = onEdt {
    bookmarkBackend.removeBookmark(char)
  }

  override suspend fun getBookmarkForMark(char: Char): BookmarkInfo? = onEdt {
    bookmarkBackend.getBookmarkForMark(char)
  }

  override suspend fun getAllBookmarks(): List<BookmarkInfo> = onEdt {
    bookmarkBackend.getAllBookmarks()
  }
}
