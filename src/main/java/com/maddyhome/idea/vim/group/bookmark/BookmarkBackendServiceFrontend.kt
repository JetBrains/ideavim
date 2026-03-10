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
import com.maddyhome.idea.vim.group.rpc

/**
 * Unified frontend [BookmarkBackendService] that always uses RPC.
 *
 * Works in both monolith and split mode because backend RPC handlers
 * use `isDispatchThread` to avoid `withContext(EDT)` when already on EDT.
 */
internal class BookmarkBackendServiceFrontend : BookmarkBackendService {

  override fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    virtualFileId: VirtualFileId,
    projectId: ProjectId?,
  ): BookmarkInfo? {
    return rpc { BookmarkRemoteApi.getInstance().createOrGetSystemMark(char, line, col, virtualFileId, projectId) }
  }

  override fun removeBookmark(char: Char) {
    rpc { BookmarkRemoteApi.getInstance().removeBookmark(char) }
  }

  override fun getBookmarkForMark(char: Char): BookmarkInfo? {
    return rpc { BookmarkRemoteApi.getInstance().getBookmarkForMark(char) }
  }

  override fun getAllBookmarks(): List<BookmarkInfo> {
    return rpc { BookmarkRemoteApi.getInstance().getAllBookmarks() }
  }

  companion object {
    @JvmStatic
    fun getInstance(): BookmarkBackendServiceFrontend =
      service<BookmarkBackendService>() as BookmarkBackendServiceFrontend
  }
}
