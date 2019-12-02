package com.maddyhome.idea.vim.common

import com.intellij.ide.bookmarks.Bookmark
import com.intellij.ide.bookmarks.BookmarkManager
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.group.MarkGroup
import java.lang.ref.WeakReference

interface Mark {
  val key: Char
  val logicalLine: Int
  val col: Int
  val filename: String
  val protocol: String

  fun isClear(): Boolean
  fun clear()
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
