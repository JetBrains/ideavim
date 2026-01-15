/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.textobjindent

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.TextObjectRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.api

/**
 * Port of vim-indent-object:
 * [vim-indent-object](https://github.com/michaeljsmith/vim-indent-object)
 *
 * vim-indent-object provides these text objects based on the cursor line's indentation:
 *
 *  * `ai` **A**n **I**ndentation level and line above.
 *  * `ii` **I**nner **I**ndentation level (no line above).
 *  * `aI` **A**n **I**ndentation level and lines above and below.
 *  * `iI` **I**nner **I**ndentation level (no lines above and below). Synonym of `ii`
 *
 * See also the reference manual for more details at:
 * [indent-object.txt](https://github.com/michaeljsmith/vim-indent-object/blob/master/doc/indent-object.txt)
 */
class VimIndentObject : VimExtension {
  override fun getName(): String = "textobj-indent"

  override fun init() {
    val api = api()
    api.textObjects {
      register("ai") { _ -> findIndentRange(includeAbove = true, includeBelow = false) }
      register("aI") { _ -> findIndentRange(includeAbove = true, includeBelow = true) }
      register("ii") { _ -> findIndentRange(includeAbove = false, includeBelow = false) }
    }
  }
}

private fun VimApi.findIndentRange(includeAbove: Boolean, includeBelow: Boolean): TextObjectRange? {
  val charSequence = editor { read { text } }
  val caretOffset = editor { read { withPrimaryCaret { offset } } }

  // Part 1: Find the start of the caret line.
  var caretLineStartOffset = caretOffset
  var accumulatedWhitespace = 0
  while (--caretLineStartOffset >= 0) {
    val ch = charSequence[caretLineStartOffset]
    if (ch == ' ' || ch == '\t') {
      ++accumulatedWhitespace
    } else if (ch == '\n') {
      ++caretLineStartOffset
      break
    } else {
      accumulatedWhitespace = 0
    }
  }
  if (caretLineStartOffset < 0) {
    caretLineStartOffset = 0
  }

  // `caretLineStartOffset` points to the first character in the line where the caret is located.

  // Part 2: Compute the indentation level of the caret line.
  // This is done as a separate step so that it works even when the caret is inside the indentation.
  var offset = caretLineStartOffset
  var indentSize = 0
  while (offset < charSequence.length) {
    val ch = charSequence[offset]
    if (ch == ' ' || ch == '\t') {
      ++indentSize
      ++offset
    } else {
      break
    }
  }

  // `indentSize` contains the amount of indent to be used for the text object range to be returned.
  var upperBoundaryOffset: Int? = null
  // Part 3: Find a line above the caret line, that has an indentation lower than `indentSize`.
  var pos1 = caretLineStartOffset - 1
  var isUpperBoundaryFound = false
  while (upperBoundaryOffset == null) {
    // 3.1: Going backwards from `caretLineStartOffset`, find the first non-whitespace character.
    while (--pos1 >= 0) {
      val ch = charSequence[pos1]
      if (ch != ' ' && ch != '\t' && ch != '\n') {
        break
      }
    }
    // 3.2: Find the indent size of the line with this non-whitespace character and check against `indentSize`.
    accumulatedWhitespace = 0
    while (--pos1 >= 0) {
      val ch = charSequence[pos1]
      if (ch == ' ' || ch == '\t') {
        ++accumulatedWhitespace
      } else if (ch == '\n') {
        if (accumulatedWhitespace < indentSize) {
          upperBoundaryOffset = pos1 + 1
          isUpperBoundaryFound = true
        }
        break
      } else {
        accumulatedWhitespace = 0
      }
    }
    if (pos1 < 0) {
      // Reached start of the buffer.
      upperBoundaryOffset = 0
      isUpperBoundaryFound = accumulatedWhitespace < indentSize
    }
  }

  // Now `upperBoundaryOffset` marks the beginning of an `ai` text object.
  if (isUpperBoundaryFound && !includeAbove) {
    while (++upperBoundaryOffset < charSequence.length) {
      val ch = charSequence[upperBoundaryOffset]
      if (ch == '\n') {
        ++upperBoundaryOffset
        break
      }
    }
    while (charSequence[upperBoundaryOffset] == '\n') {
      ++upperBoundaryOffset
    }
  }

  // Part 4: Find the end of the caret line.
  var caretLineEndOffset = caretOffset
  while (++caretLineEndOffset < charSequence.length) {
    val ch = charSequence[caretLineEndOffset]
    if (ch == '\n') {
      ++caretLineEndOffset
      break
    }
  }

  // `caretLineEndOffset` points to the first character in the line below caret line.
  var lowerBoundaryOffset: Int? = null
  // Part 5: Find a line below the caret line, that has an indentation lower than `indentSize`.
  var pos2 = caretLineEndOffset - 1
  var isLowerBoundaryFound = false
  while (lowerBoundaryOffset == null) {
    var accumulatedWhitespace2 = 0
    var lastNewlinePos = caretLineEndOffset - 1
    var isInIndent = true
    while (++pos2 < charSequence.length) {
      val ch = charSequence[pos2]
      if (isIndentChar(ch) && isInIndent) {
        ++accumulatedWhitespace2
      } else if (ch == '\n') {
        accumulatedWhitespace2 = 0
        lastNewlinePos = pos2
        isInIndent = true
      } else {
        if (isInIndent && accumulatedWhitespace2 < indentSize) {
          lowerBoundaryOffset = lastNewlinePos
          isLowerBoundaryFound = true
          break
        }
        isInIndent = false
      }
    }
    if (pos2 >= charSequence.length) {
      // Reached end of the buffer.
      lowerBoundaryOffset = charSequence.length - 1
    }
  }

  // Now `lowerBoundaryOffset` marks the end of an `ii` text object.
  if (isLowerBoundaryFound && includeBelow) {
    while (++lowerBoundaryOffset < charSequence.length) {
      val ch = charSequence[lowerBoundaryOffset]
      if (ch == '\n') {
        break
      }
    }
  }

  // Convert offsets to line numbers for LineWise result
  val startLine = editor { read { getLine(upperBoundaryOffset).number } }
  val endLine = editor { read { getLine(lowerBoundaryOffset).number } }
  return TextObjectRange.LineWise(startLine, endLine)
}

private fun isIndentChar(ch: Char): Boolean = ch == ' ' || ch == '\t'
