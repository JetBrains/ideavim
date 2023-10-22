/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

import com.intellij.ide.bookmark.BookmarkType
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.bookmark.LineBookmark
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import java.lang.ref.WeakReference

internal class IntellijMark(bookmark: LineBookmark, override val col: Int, project: Project?) : Mark {

  private val project: WeakReference<Project?> = WeakReference(project)

  override val key = BookmarksManager.getInstance(project)?.getType(bookmark)?.mnemonic!!
  override val line: Int
    get() = getMark()?.line ?: 0
  override val filepath: String
    get() = getMark()?.file?.path ?: ""
  override val protocol: String?
    get() = getMark()?.file?.let { VirtualFileManager.extractProtocol(it.url) } ?: ""

  fun clear() {
    val mark = getMark() ?: return
    getProject()?.let { project -> BookmarksManager.getInstance(project)?.remove(mark) }
  }

  private fun getMark(): LineBookmark? =
    getProject()?.let { project ->
      BookmarksManager.getInstance(project)?.getBookmark(BookmarkType.get(key)) as? LineBookmark
    }

  private fun getProject(): Project? {
    val proj = project.get() ?: return null
    if (proj.isDisposed) return null
    return proj
  }
}
