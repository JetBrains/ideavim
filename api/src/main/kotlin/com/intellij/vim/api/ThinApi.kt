/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

enum class RegisterType {
  LINE,
  CHAR,
  BLOCK,
}

val RegisterType.isLine: Boolean
  get() = this == RegisterType.LINE

data class RegisterData(
  val text: String,
  val type: RegisterType
)

enum class TextSelectionType {
  CHARACTER_WISE,
  LINE_WISE,
  BLOCK_WISE,
}

data class Range(val start: Int, val end: Int)

typealias CaretData = Pair<CaretId, CaretInfo>

@JvmInline
value class CaretId(val id: String)

data class CaretInfo(
  val offset: Int,
  val selection: Pair<Int, Int>?,
)

enum class Mode {
  NORMAL,
  VISUAL,
  SELECT,
  OP_PENDING,
  INSERT,
  COMMAND
}