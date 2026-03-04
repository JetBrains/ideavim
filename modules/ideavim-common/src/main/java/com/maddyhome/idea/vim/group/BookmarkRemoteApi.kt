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
 * RPC interface for IDE bookmark operations on the backend.
 *
 * In split mode, the thin-client frontend cannot create IDE bookmarks because
 * [com.intellij.ide.bookmark.BookmarksManager] has no bookmark groups on the
 * frontend process. This API forwards bookmark creation to the backend where
 * the full bookmarks infrastructure is available.
 *
 * Called from [VimMarkServiceImpl] when `ideamarks` is enabled.
 */
@Rpc
@ApiStatus.Internal
interface BookmarkRemoteApi : RemoteApi<Unit> {
  /**
   * Creates or retrieves an IDE bookmark on the backend.
   *
   * @param char the mark character (e.g. 'A')
   * @param line the 0-based line number
   * @param filePath the file path where the bookmark should be placed
   * @param projectId project identifier for resolving the correct project on backend
   * @return bookmark info if created/found, null if bookmark creation failed
   */
  suspend fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    filePath: String,
    projectId: String?,
    protocol: String? = null,
  ): BookmarkInfo?

  /**
   * Removes an IDE bookmark on the backend.
   *
   * @param char the mark character (e.g. 'A')
   */
  suspend fun removeBookmark(char: Char)

  /**
   * Retrieves the current IDE bookmark for a given mark character.
   * Returns the bookmark's current position from BookmarksManager,
   * which is the source of truth when ideamarks is enabled.
   *
   * Used in split mode to resolve global marks on demand, since
   * [com.intellij.ide.bookmark.BookmarksListener] events don't fire
   * on the thin client.
   *
   * @param char the mark character (e.g. 'A')
   * @return bookmark info if a bookmark with this mnemonic exists, null otherwise
   */
  suspend fun getBookmarkForMark(char: Char): BookmarkInfo?

  /**
   * Retrieves all mnemonic IDE bookmarks (A-Z, 0-9) from the backend.
   * Used for the `:marks` command to show all global marks in split mode.
   *
   * @return list of all existing mnemonic bookmarks
   */
  suspend fun getAllBookmarks(): List<BookmarkInfo>

  companion object {
    @JvmStatic
    suspend fun getInstance(): BookmarkRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<BookmarkRemoteApi>())
    }
  }
}
