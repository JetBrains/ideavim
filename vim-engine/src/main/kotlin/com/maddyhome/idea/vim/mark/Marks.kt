/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.BufferPosition
import org.jetbrains.annotations.NonNls

interface Mark {
  val key: Char
  val logicalLine: Int
  val col: Int
  val filename: String
  val protocol: String?

  fun isClear(): Boolean
  fun clear()

  fun offset(editor: VimEditor): Int = editor.logicalPositionToOffset(BufferPosition(logicalLine, col))

  object KeySorter : Comparator<Mark> {
    @NonNls
    private const val ORDER = "'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"[]^.<>"

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
  override val protocol: String?,
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
