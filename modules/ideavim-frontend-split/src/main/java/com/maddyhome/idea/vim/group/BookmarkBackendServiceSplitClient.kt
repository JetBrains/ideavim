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
 * Split-mode (thin client) [BookmarkBackendService] implementation.
 *
 * Every operation is forwarded to the backend via [BookmarkRemoteApi] RPC.
 * No bookmark logic lives here — it is all in [VimMarkServiceImpl] on the frontend.
 */
internal class BookmarkBackendServiceSplitClient : BookmarkBackendService {

  override fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    filePath: String,
    projectId: String?,
    protocol: String?,
  ): BookmarkInfo? {
    return rpc { createOrGetSystemMark(char, line, col, filePath, projectId, protocol) }
  }

  override fun removeBookmark(char: Char) {
    rpc { removeBookmark(char) }
  }

  override fun getBookmarkForMark(char: Char): BookmarkInfo? {
    return rpc { getBookmarkForMark(char) }
  }

  override fun getAllBookmarks(): List<BookmarkInfo> {
    return rpc { getAllBookmarks() }
  }

  private fun <T> rpc(block: suspend BookmarkRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { BookmarkRemoteApi.getInstance().block() }
  }
}
