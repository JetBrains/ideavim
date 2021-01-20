/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.common

import com.intellij.ide.bookmarks.Bookmark
import com.intellij.ide.bookmarks.BookmarkManager
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.group.MarkGroup
import org.jetbrains.annotations.NonNls
import java.lang.ref.WeakReference

interface Mark {
  val key: Char
  val logicalLine: Int
  val col: Int
  val filename: String
  val protocol: String

  fun isClear(): Boolean
  fun clear()

  object KeySorter : Comparator<Mark> {
    @NonNls private const val ORDER = "'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"[]^.<>"

    override fun compare(o1: Mark, o2: Mark): Int {
      return ORDER.indexOf(o1.key) - ORDER.indexOf(o2.key)
    }
  }
}

data class VimMark(
  override val key: Char,
  override var logicalLine: Int,
  override val col: Int,
  override val filename: String,
  override val protocol: String
) : Mark {

  private var cleared = false

  override fun isClear(): Boolean = cleared

  override fun clear() {
    cleared = true
  }

  companion object {
    @JvmStatic
    fun create(key: Char?, logicalLine: Int?, col: Int?, filename: String?, protocol: String?): VimMark? {
      return VimMark(
        key ?: return null,
        logicalLine ?: return null,
        col ?: 0,
        filename ?: return null,
        protocol ?: ""
      )
    }
  }
}

class IntellijMark(bookmark: Bookmark, override val col: Int, project: Project?) : Mark {

  private val project: WeakReference<Project?> = WeakReference(project)

  override val key = bookmark.mnemonic
  override val logicalLine: Int
    get() = getMark()?.line ?: 0
  override val filename: String
    get() = getMark()?.file?.path ?: ""
  override val protocol: String
    get() = getMark()?.file?.let { MarkGroup.extractProtocol(it) } ?: ""

  override fun isClear(): Boolean = getMark()?.isValid?.not() ?: false
  override fun clear() {
    val mark = getMark() ?: return
    getProject()?.let { project -> BookmarkManager.getInstance(project).removeBookmark(mark) }
  }

  private fun getMark(): Bookmark? =
    getProject()?.let { project -> BookmarkManager.getInstance(project).findBookmarkForMnemonic(key) }

  private fun getProject(): Project? {
    val proj = project.get() ?: return null
    if (proj.isDisposed) return null
    return proj
  }
}
