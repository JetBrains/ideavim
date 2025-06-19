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

interface HighlightId

data class Color(
  val hexCode: String
) {
  val r: Int
    get() = hexCode.substring(1..2).toInt(16)

  val g: Int
    get() = hexCode.substring(3..4).toInt(16)

  val b: Int
    get() = hexCode.substring(5..6).toInt(16)

  val a: Int
    get() = if (hexCode.length == 9) hexCode.substring(7..8).toInt(16) else 255

  init {
    require(hexCode.matches(Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$"))) {
      "Hex code should be in format #RRGGBB[AA]"
    }
  }

  companion object {
    fun fromRgba(r: Int, g: Int, b: Int, a: Int = 255): Color {
      val hexCode = String.format("#%02x%02x%02x%02x", r, g, b, a)
      return Color(hexCode)
    }
  }
}

class VimPluginException(message: String) : Exception(message)