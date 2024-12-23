/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.mark.Mark.KeySorter.ORDER
import org.jetbrains.annotations.NonNls

interface Mark {
  val key: Char
  val line: Int // 0-based
  val col: Int // 0-based
  val filepath: String
  val protocol: String

  fun offset(editor: VimEditor): Int = editor.bufferPositionToOffset(BufferPosition(line, col))

  object KeySorter : Comparator<Mark> {
    @NonNls
    private const val ORDER = "'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"[]^.<>"

    override fun compare(o1: Mark, o2: Mark): Int {
      return ORDER.indexOf(o1.key) - ORDER.indexOf(o2.key)
    }
  }
  // Same as in BufferPosition.
  // TODO: Consider having a shared Interface / comparator for Mark and BufferPosition to avoid this duplication.
  object PositionSorter: Comparator<Mark> {
    override fun compare(o1: Mark, o2: Mark): Int {
      return if (o1.line != o2.line) o1.line - o2.line else o1.col - o2.col
    }
  }
}

data class VimMark(
  override val key: Char,
  override var line: Int,
  override val col: Int,
  override val filepath: String,
  override val protocol: String,
) : Mark {
  companion object {
    @JvmStatic
    fun create(key: Char?, line: Int?, col: Int?, filename: String?, protocol: String?): VimMark? {
      return VimMark(
        key ?: return null,
        line ?: return null,
        col ?: 0,
        filename ?: return null,
        protocol ?: "",
      )
    }
  }
}
