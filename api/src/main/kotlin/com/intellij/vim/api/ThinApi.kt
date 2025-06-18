/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

val TextType.isLine: Boolean
  get() = this == TextType.LINE_WISE

enum class TextType {
  CHARACTER_WISE,
  LINE_WISE,
  BLOCK_WISE,
}

class TextInfo(
  val text: String,
  val type: TextType = TextType.CHARACTER_WISE
)

sealed interface Range {
  data class Simple(val start: Int, val end: Int): Range

  data class Block(val ranges: Array<Simple>): Range {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Block
      return ranges.contentEquals(other.ranges)
    }

    override fun hashCode(): Int {
      return ranges.contentHashCode()
    }
  }
}


data class Line(val number: Int, val text: String, val start: Int, val end: Int)

typealias CaretData = Pair<CaretId, CaretInfo>

@JvmInline
value class CaretId(val id: String)

data class CaretInfo(
  val offset: Int,
  val selection: Pair<Int, Int>?,
)

interface HighlighterId

data class Color(
  val hexCode: String
)

class VimPluginException(message: String) : Exception(message)