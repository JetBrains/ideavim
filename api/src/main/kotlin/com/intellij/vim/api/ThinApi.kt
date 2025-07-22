/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

/**
 * Represents the type of text selection in Vim.
 */
enum class TextType {
  /**
   * Character-wise selection mode, where text is selected character by character.
   */
  CHARACTER_WISE,

  /**
   * Line-wise selection mode, where text is selected line by line.
   */
  LINE_WISE,

  /**
   * Block-wise selection mode, where text is selected in a rectangular block.
   */
  BLOCK_WISE,
}

/**
 * Represents a line of text in the editor.
 *
 * @property number The line number (0-based or 1-based depending on context).
 * @property text The content of the line.
 * @property start The offset of the first character in the line.
 * @property end The offset after the last character in the line.
 */
data class Line(val number: Int, val text: String, val start: Int, val end: Int)

/**
 * Represents a caret with its associated information.
 * A pair of [CaretId] and [CaretInfo].
 */
typealias CaretData = Pair<CaretId, CaretInfo>

/**
 * A unique identifier for a caret in the editor.
 *
 * @property id The string representation of the caret identifier.
 */
@JvmInline
value class CaretId(val id: String)

/**
 * Contains information about a caret's position and selection.
 *
 * @property offset The current offset (position) of the caret in the document.
 * @property selection The selection range as a pair of start and end offsets, or null if no selection.
 */
data class CaretInfo(
  val offset: Int,
  val selection: Pair<Int, Int>?,
)

/**
 * Represents an identifier for a highlight in the editor.
 * Used for tracking and managing highlights applied to text.
 */
interface HighlightId
