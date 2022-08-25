/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group

import com.intellij.ide.bookmark.Bookmark
import com.intellij.ide.bookmark.BookmarkType
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.bookmark.LineBookmark
import com.intellij.ide.bookmark.providers.LineBookmarkProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.options.OptionScope.LOCAL
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService

class SystemMarks {
  companion object {
    @JvmStatic
    fun createOrGetSystemMark(ch: Char, line: Int, editor: Editor): LineBookmark? {
      if (!VimPlugin.getOptionService().isSet(LOCAL(IjVimEditor(editor)), IjVimOptionService.ideamarksName, IjVimOptionService.ideamarksName)) return null

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
  val lineBookmarkProvider = LineBookmarkProvider.find(this) ?: return null
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
