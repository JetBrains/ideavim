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
import com.intellij.ide.vfs.VirtualFileId
import com.intellij.ide.vfs.virtualFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.maddyhome.idea.vim.api.VimMarkService

/**
 * Direct [BookmarkBackendService] implementation using IntelliJ bookmark APIs.
 *
 * Runs in backend + monolith mode. In split mode, [BookmarkRemoteApiImpl] delegates
 * to this class for the actual bookmark operations.
 */
internal class BookmarkBackendServiceImpl : BookmarkBackendService {

  // LineBookmark doesn't store column, so we track it separately
  private val columnByMark = mutableMapOf<Char, Int>()

  override fun createOrGetSystemMark(
    char: Char,
    line: Int,
    col: Int,
    virtualFileId: VirtualFileId,
    projectId: ProjectId?,
  ): BookmarkInfo? {
    val type = BookmarkType.get(char)
    if (type == BookmarkType.DEFAULT) return null

    val project = projectId?.findProjectOrNull() ?: return null
    val bookmarksManager = BookmarksManager.getInstance(project) ?: return null

    // If a bookmark with this mnemonic already exists, check if it's at the right line
    val existing = bookmarksManager.getBookmark(type)
    if (existing != null) {
      if (existing is LineBookmark && existing.line == line) {
        columnByMark[char] = col
        return BookmarkInfo(
          key = char,
          line = existing.line,
          col = col,
          filepath = existing.file.path,
          protocol = existing.file.fileSystem.protocol,
        )
      }
      bookmarksManager.remove(existing)
    }

    // Create a new line bookmark — find editor for the virtual file
    val vf = virtualFileId.virtualFile() ?: return null
    val editor = FileEditorManager.getInstance(project).getAllEditors(vf)
      .filterIsInstance<TextEditor>()
      .firstOrNull()
      ?.editor ?: return null
    val lineBookmarkProvider = LineBookmarkProvider.Util.find(project) ?: return null
    val bookmark = lineBookmarkProvider.createBookmark(editor, line) as? LineBookmark ?: return null

    val group = bookmarksManager.defaultGroup
      ?: bookmarksManager.getGroup("IdeaVim")
      ?: bookmarksManager.addGroup("IdeaVim", true)
      ?: return null
    if (!group.canAdd(bookmark)) return null
    group.add(bookmark, type)

    columnByMark[char] = col
    return BookmarkInfo(
      key = char,
      line = bookmark.line,
      col = col,
      filepath = bookmark.file.path,
      protocol = bookmark.file.fileSystem.protocol,
    )
  }

  override fun removeBookmark(char: Char) {
    val type = BookmarkType.get(char)
    if (type == BookmarkType.DEFAULT) return
    columnByMark.remove(char)
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
          col = columnByMark[char] ?: 0,
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
              col = columnByMark[typeChar] ?: 0,
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
