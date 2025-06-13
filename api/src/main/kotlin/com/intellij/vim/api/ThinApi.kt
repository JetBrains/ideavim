/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

val TextSelectionType.isLine: Boolean
  get() = this == TextSelectionType.LINE_WISE

data class RegisterData(
  val text: String,
  val type: TextSelectionType
)

enum class TextSelectionType {
  CHARACTER_WISE,
  LINE_WISE,
  BLOCK_WISE,
}

class TextInfo(
  val text: String,
  val type: TextSelectionType = TextSelectionType.CHARACTER_WISE
)

data class Range(val start: Int, val end: Int)

typealias CaretData = Pair<CaretId, CaretInfo>

@JvmInline
value class CaretId(val id: String)

data class CaretInfo(
  val offset: Int,
  val selection: Pair<Int, Int>?,
)

interface Highlighter

data class Color(
  val hexCode: String
)

class VimPluginException(message: String) : Exception(message)