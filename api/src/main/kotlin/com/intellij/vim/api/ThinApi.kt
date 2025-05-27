/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.state.mode.SelectionType

enum class RegisterType {
  LINE,
  CHAR,
  BLOCK,
}

val RegisterType.isLine: Boolean
  get() = this == RegisterType.LINE

fun RegisterType.toSelectionType(): SelectionType {
  return when (this) {
    RegisterType.LINE -> SelectionType.LINE_WISE
    RegisterType.CHAR -> SelectionType.CHARACTER_WISE
    RegisterType.BLOCK -> SelectionType.BLOCK_WISE
  }
}

fun SelectionType.toRegisterType(): RegisterType {
  return when (this) {
    SelectionType.CHARACTER_WISE -> RegisterType.CHAR
    SelectionType.LINE_WISE -> RegisterType.LINE
    SelectionType.BLOCK_WISE -> RegisterType.BLOCK
  }
}

enum class TextSelectionType {
  CHARACTER_WISE,
  LINE_WISE,
  BLOCK_WISE,
}

fun SelectionType.toTextSelectionType(): TextSelectionType {
  return when (this) {
    SelectionType.CHARACTER_WISE -> TextSelectionType.CHARACTER_WISE
    SelectionType.LINE_WISE -> TextSelectionType.LINE_WISE
    SelectionType.BLOCK_WISE -> TextSelectionType.BLOCK_WISE
  }
}

fun TextSelectionType.toSelectionType(): SelectionType {
  return when (this) {
    TextSelectionType.CHARACTER_WISE -> SelectionType.CHARACTER_WISE
    TextSelectionType.LINE_WISE -> SelectionType.LINE_WISE
    TextSelectionType.BLOCK_WISE -> SelectionType.BLOCK_WISE
  }
}

typealias CaretData = Pair<CaretId, CaretInfo>

@JvmInline
value class CaretId(val id: String)

val VimCaret.caretId: CaretId
  get() = CaretId(this.id)

val VimCaret.caretInfo: CaretInfo
  get() = CaretInfo(
    this.offset,
    if (hasSelection()) selectionStart to selectionEnd else null
  )

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

fun Mode.toMappingMode(): MappingMode {
  return when (this) {
    Mode.NORMAL -> MappingMode.NORMAL
    Mode.VISUAL -> MappingMode.VISUAL
    Mode.SELECT -> MappingMode.SELECT
    Mode.OP_PENDING -> MappingMode.OP_PENDING
    Mode.INSERT -> MappingMode.INSERT
    Mode.COMMAND -> MappingMode.CMD_LINE
  }
}

fun MappingMode.toMode(): Mode {
  return when (this) {
    MappingMode.NORMAL -> Mode.NORMAL
    MappingMode.VISUAL -> Mode.VISUAL
    MappingMode.SELECT -> Mode.SELECT
    MappingMode.OP_PENDING -> Mode.OP_PENDING
    MappingMode.INSERT -> Mode.INSERT
    MappingMode.CMD_LINE -> Mode.COMMAND
  }
}