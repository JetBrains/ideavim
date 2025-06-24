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
import com.intellij.vim.api.Mark
import com.intellij.vim.api.Path
import com.intellij.vim.api.Range
import com.intellij.vim.api.TextType
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.intellij.vim.api.Jump as ApiJump
import com.maddyhome.idea.vim.mark.Jump as EngineJump
import com.maddyhome.idea.vim.mark.Mark as EngineMark

fun SelectionType.toTextSelectionType(): TextType {
  return when (this) {
    SelectionType.CHARACTER_WISE -> TextType.CHARACTER_WISE
    SelectionType.LINE_WISE -> TextType.LINE_WISE
    SelectionType.BLOCK_WISE -> TextType.BLOCK_WISE
  }
}

fun TextType.toSelectionType(): SelectionType {
  return when (this) {
    TextType.CHARACTER_WISE -> SelectionType.CHARACTER_WISE
    TextType.LINE_WISE -> SelectionType.LINE_WISE
    TextType.BLOCK_WISE -> SelectionType.BLOCK_WISE
  }
}

val VimCaret.caretId: CaretId
  get() = CaretId(this.id)

val VimCaret.caretInfo: CaretInfo
  get() = CaretInfo(
    this.offset,
    if (hasSelection()) selectionStart to selectionEnd else null
  )

fun TextRange.toRange(): Range.Simple {
  return Range.Simple(startOffset, endOffset)
}

fun EngineMark.toApiMark(): Mark {
  val mark = this
  return object : Mark {
    override val key: Char = mark.key
    override val line: Int = mark.line
    override val col: Int = mark.col
    override val filePath: Path
      get() = object : Path {
        override val protocol: String
          get() = filePath.protocol
        override val path: Array<String>
          get() = filePath.path
      }
  }
}

fun EngineJump.toApiJump(): ApiJump {
  return object : ApiJump {
    override val line: Int = this@toApiJump.line
    override val col: Int = this@toApiJump.col
    override val filepath: Path
      get() = TODO("Not yet implemented")
  }
}