/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.ide.bookmark.Bookmark
import com.intellij.ide.bookmark.BookmarkType
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.bookmark.LineBookmark
import com.intellij.ide.bookmark.providers.LineBookmarkProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions

internal class SystemMarks {
  companion object {
    @JvmStatic
    fun createOrGetSystemMark(ch: Char, line: Int, editor: Editor): LineBookmark? {
      if (!injector.globalIjOptions().ideamarks) return null

      val project = editor.project ?: return null
      val type = BookmarkType.get(ch)
      if (type == BookmarkType.DEFAULT) return null

      val bookmarksManager = BookmarksManager.getInstance(project) ?: return null
      val foundBookmark = bookmarksManager.getBookmark(type)
      if (foundBookmark != null) {
        if (foundBookmark is LineBookmark && foundBookmark.line == line) {
          return foundBookmark
        }
        bookmarksManager.remove(foundBookmark)
      }

      return project.createLineBookmark(editor, line, ch)
    }
  }
}

internal fun Project.createLineBookmark(editor: Editor, line: Int, mnemonic: Char): LineBookmark? {
  val bookmarksManager = BookmarksManager.getInstance(this) ?: return null
  val lineBookmarkProvider = LineBookmarkProvider.Util.find(this) ?: return null
  val bookmark = lineBookmarkProvider.createBookmark(editor, line) as LineBookmark? ?: return null
  val type = BookmarkType.get(mnemonic)
  if (type == BookmarkType.DEFAULT) return null

  val group = bookmarksManager.defaultGroup
    ?: bookmarksManager.getGroup("IdeaVim")
    ?: bookmarksManager.addGroup("IdeaVim", true)
    ?: return null
  if (group.canAdd(bookmark)) {
    group.add(bookmark, type)
    return bookmark
  }
  return null
}

internal fun Bookmark.mnemonic(project: Project?): Char {
  return BookmarksManager.getInstance(project)?.getType(this)!!.mnemonic
}
