/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.Color
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.TextSelectionType
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.lang.String
import kotlin.text.removePrefix
import kotlin.text.toLong
import kotlin.to
import java.awt.Color as AwtColor

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

val VimCaret.caretId: CaretId
  get() = CaretId(this.id)

val VimCaret.caretInfo: CaretInfo
  get() = CaretInfo(
    this.offset,
    if (hasSelection()) selectionStart to selectionEnd else null
  )

fun Color.toAwtColor(): AwtColor {
  val argb = hexCode.removePrefix("#").toLong(16)
  val hasAlpha = hexCode.length > 7

  val r = (argb shr 16).toInt() and 0xFF
  val g = (argb shr 8).toInt() and 0xFF
  val b = argb.toInt() and 0xFF
  val a = if (hasAlpha) (argb shr 24).toInt() and 0xFF else 0xFF

  return AwtColor(r, g, b, a)
}

fun AwtColor.toHexColor(): Color {
  val hexColor = String.format("#%02x%02x%02x%02x", alpha, red, green, blue)
  return Color(hexColor)
}