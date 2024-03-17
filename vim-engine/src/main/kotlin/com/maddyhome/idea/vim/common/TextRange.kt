/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.common

import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls
import kotlin.math.max
import kotlin.math.min

/**
 * Please prefer [com.maddyhome.idea.vim.group.visual.VimSelection] for visual selection
 */
public data class TextRange(public val startOffsets: IntArray, public val endOffsets: IntArray) {
  public constructor(start: Int, end: Int) : this(intArrayOf(start), intArrayOf(end))

  public val isMultiple: Boolean
    get() = startOffsets.size > 1

  public val maxLength: Int
    get() {
      var max = 0
      for (i in 0 until size()) {
        max = max(max, endOffsets[i] - startOffsets[i])
      }
      return max
    }

  public val selectionCount: Int
    get() {
      var res = 0
      for (i in 0 until size()) {
        res += endOffsets[i] - startOffsets[i]
      }
      return res
    }

  public fun size(): Int = startOffsets.size

  public val startOffset: Int
    get() = startOffsets.first()

  public val endOffset: Int
    get() = endOffsets.last()

  public fun normalize(): TextRange {
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
  public fun normalize(fileSize: Int): Boolean {
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

  public operator fun contains(offset: Int): Boolean {
    return (0 until size()).any { offset in startOffsets[it] until endOffsets[it] }
  }

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as TextRange

    if (!startOffsets.contentEquals(other.startOffsets)) return false
    return endOffsets.contentEquals(other.endOffsets)
  }

  override fun hashCode(): Int {
    var result = startOffsets.contentHashCode()
    result = 31 * result + endOffsets.contentHashCode()
    return result
  }
}
