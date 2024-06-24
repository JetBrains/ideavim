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
import org.jetbrains.annotations.NonNls

public interface Mark {
  public val key: Char
  public val line: Int // 0-based
  public val col: Int // 0-based
  public val filepath: String
  public val protocol: String

  public fun offset(editor: VimEditor): Int = editor.bufferPositionToOffset(BufferPosition(line, col))

  public object KeySorter : Comparator<Mark> {
    @NonNls
    private const val ORDER = "'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"[]^.<>"

    override fun compare(o1: Mark, o2: Mark): Int {
      return ORDER.indexOf(o1.key) - ORDER.indexOf(o2.key)
    }
  }
}

public data class VimMark(
  override val key: Char,
  override var line: Int,
  override val col: Int,
  override val filepath: String,
  override val protocol: String,
) : Mark {
  public companion object {
    @JvmStatic
    public fun create(key: Char?, line: Int?, col: Int?, filename: String?, protocol: String?): VimMark? {
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
