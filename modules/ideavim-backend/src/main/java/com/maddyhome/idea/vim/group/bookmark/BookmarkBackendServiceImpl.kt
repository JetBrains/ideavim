/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.ide.bookmark.BookmarkType
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.bookmark.LineBookmark
import com.intellij.ide.bookmark.providers.LineBookmarkProvider
import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.group.BookmarkBackendService
import com.maddyhome.idea.vim.group.BookmarkInfo
import com.maddyhome.idea.vim.group.findEditorByFilePath
import com.maddyhome.idea.vim.group.findProjectById

/**
 * Direct [BookmarkBackendService] implementation using IntelliJ bookmark APIs.
 *
 * Runs in backend + monolith mode. In split mode, [BookmarkRemoteApiImpl] delegates
 * to this class for the actual bookmark operations.
 */
internal class BookmarkBackendServiceImpl : BookmarkBackendService {

  override fun createOrGetSystemMark(
    char: Char,
    line: Int,
    filePath: String,
    projectId: String?,
  ): BookmarkInfo? {
    val type = BookmarkType.get(char)
    if (type == BookmarkType.DEFAULT) return null

    val project = findProjectById(projectId) ?: return null
    val bookmarksManager = BookmarksManager.getInstance(project) ?: return null

    // If a bookmark with this mnemonic already exists, check if it's at the right line
    val existing = bookmarksManager.getBookmark(type)
    if (existing != null) {
      if (existing is LineBookmark && existing.line == line) {
        return BookmarkInfo(
          key = char,
          line = existing.line,
          col = 0,
          filepath = existing.file.path,
          protocol = existing.file.fileSystem.protocol,
        )
      }
      bookmarksManager.remove(existing)
    }

    // Create a new line bookmark
    val editor = findEditorByFilePath(project, filePath) ?: return null
    val lineBookmarkProvider = LineBookmarkProvider.Util.find(project) ?: return null
    val bookmark = lineBookmarkProvider.createBookmark(editor, line) as? LineBookmark ?: return null

    val group = bookmarksManager.defaultGroup
      ?: bookmarksManager.getGroup("IdeaVim")
      ?: bookmarksManager.addGroup("IdeaVim", true)
      ?: return null
    if (!group.canAdd(bookmark)) return null
    group.add(bookmark, type)

    return BookmarkInfo(
      key = char,
      line = bookmark.line,
      col = 0,
      filepath = bookmark.file.path,
      protocol = bookmark.file.fileSystem.protocol,
    )
  }

  override fun removeBookmark(char: Char) {
    val type = BookmarkType.get(char)
    if (type == BookmarkType.DEFAULT) return
    for (project in ProjectManager.getInstance().openProjects) {
      val bookmarksManager = BookmarksManager.getInstance(project) ?: continue
      val bookmark = bookmarksManager.getBookmark(type) ?: continue
      bookmarksManager.remove(bookmark)
      return
    }
  }

  override fun getBookmarkForMark(char: Char): BookmarkInfo? {
    val type = BookmarkType.get(char)
    if (type == BookmarkType.DEFAULT) return null
    for (project in ProjectManager.getInstance().openProjects) {
      val bookmarksManager = BookmarksManager.getInstance(project) ?: continue
      val bookmark = bookmarksManager.getBookmark(type) ?: continue
      if (bookmark is LineBookmark) {
        return BookmarkInfo(
          key = char,
          line = bookmark.line,
          col = 0,
          filepath = bookmark.file.path,
          protocol = bookmark.file.fileSystem.protocol,
        )
      }
    }
    return null
  }

  override fun getAllBookmarks(): List<BookmarkInfo> {
    val result = mutableListOf<BookmarkInfo>()
    for (project in ProjectManager.getInstance().openProjects) {
      val bookmarksManager = BookmarksManager.getInstance(project) ?: continue
      for (typeChar in (VimMarkService.UPPERCASE_MARKS + VimMarkService.NUMBERED_MARKS)) {
        val type = BookmarkType.get(typeChar)
        if (type == BookmarkType.DEFAULT) continue
        val bookmark = bookmarksManager.getBookmark(type) ?: continue
        if (bookmark is LineBookmark) {
          result.add(
            BookmarkInfo(
              key = typeChar,
              line = bookmark.line,
              col = 0,
              filepath = bookmark.file.path,
              protocol = bookmark.file.fileSystem.protocol,
            )
          )
        }
      }
      break // mnemonic bookmarks are per-application, first project is sufficient
    }
    return result
  }
}
