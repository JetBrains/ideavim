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

import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls
import kotlin.math.max
import kotlin.math.min

/**
 * Please prefer [com.maddyhome.idea.vim.group.visual.VimSelection] for visual selection
 */
class TextRange(val startOffsets: IntArray, val endOffsets: IntArray) {
  constructor(start: Int, end: Int) : this(intArrayOf(start), intArrayOf(end))

  val isMultiple
    get() = startOffsets.size > 1

  val maxLength: Int
    get() {
      var max = 0
      for (i in 0 until size()) {
        max = max(max, endOffsets[i] - startOffsets[i])
      }
      return max
    }

  val selectionCount: Int
    get() {
      var res = 0
      for (i in 0 until size()) {
        res += endOffsets[i] - startOffsets[i]
      }
      return res
    }

  fun size(): Int = startOffsets.size

  val startOffset: Int
    get() = startOffsets.first()

  val endOffset: Int
    get() = endOffsets.last()

  fun normalize(): TextRange {
    normalizeIndex(0)
    return this
  }

  private fun normalizeIndex(index: Int) {
    if (index < size() && endOffsets[index] < startOffsets[index]) {
      val t = startOffsets[index]
      startOffsets[index] = endOffsets[index]
      endOffsets[index] = t
    }
  }

  @Contract(mutates = "this")
  fun normalize(fileSize: Int): Boolean {
    for (i in 0 until size()) {
      normalizeIndex(i)
      startOffsets[i] = max(0, min(startOffsets[i], fileSize))
      if (startOffsets[i] == fileSize && fileSize != 0) {
        return false
      }
      endOffsets[i] = max(0, min(endOffsets[i], fileSize))
    }
    return true
  }

  operator fun contains(offset: Int): Boolean = if (isMultiple) false else offset in startOffset until endOffset

  override fun toString(): String {
    @NonNls val sb = StringBuilder()
    sb.append("TextRange")
    sb.append("{starts=")

    var i = 0
    while (i < startOffsets.size) {
      sb.append(if (i == 0) "" else ", ").append(startOffsets[i])
      ++i
    }

    sb.append(", ends=")
    i = 0
    while (i < endOffsets.size) {
      sb.append(if (i == 0) "" else ", ").append(endOffsets[i])
      ++i
    }
    sb.append('}')
    return sb.toString()
  }
}
