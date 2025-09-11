/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex.ranges

import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.exExceptionMessage
import org.jetbrains.annotations.TestOnly
import java.util.*

/**
 * Base for all Ex command addresses
 *
 * An address is a way of specifying a line number, either explicitly by number, or with a symbol to represent current
 * line, last line, etc. An address can also have an offset, to allow for relative counting. A range is a collection of
 * addresses. Each address is separated either with a comma, or a semicolon. If separated by a semicolon, the caret is
 * moved after evaluating the line number.
 *
 * A range matching the whole file is represented by `%`, which is evaluated as two addresses, for the first and last
 * lines in the file.
 *
 * Note that an address is a one-based line number, because addresses can be also used as counts, and it is important
 * to distinguish `0` from `1`. An address for line `0` is a special line, meaning the line _before_ the first line,
 * important for e.g. `:1,3move 0`.
 *
 * See `:help range` and `:help {address}`.
 *
 * @param offset  The relative offset added or subtracted from the evaluated line number
 * @param isMove  True if the caret should be moved after evaluating this address. In the text representation, a
 *                semicolon follows this address.
 */
sealed class Address(val offset: Int, val isMove: Boolean) {

  /**
   * Gets the one-based line number specified by this address.
   *
   * If the range included an offset (`+1`, `-1`), this is applied to the returned line number.
   *
   * Note that the user will have used one-based line numbers, but internally, we use zero-based. This conversion is
   * automatically handled.
   *
   * @param editor   The editor to get the line for
   * @param caret    The caret to use for the current line or initial search line, if required
   * @return The one-based line number or -1 if unable to get the line number
   */
  fun getLine1(editor: VimEditor, caret: ImmutableVimCaret): Int {
    val line = calculateLine1(editor, caret)
    return applyOffset(line)
  }

  protected open fun applyOffset(line: Int): Int {
    return when {
      line != -1 -> line + offset
      else -> line
    }
  }

  /**
   * Calculate the line number specified by this address. Does not apply offset
   *
   * @param editor   The editor to get the line for
   * @param caret    The caret to use for initial search offset, or to get the current line, etc.
   * @return The one-based line number, -1 if unable to get the line number
   */
  protected abstract fun calculateLine1(editor: VimEditor, caret: ImmutableVimCaret): Int

  override fun toString(): String = "Range{offset=$offset, move=$isMove}"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Address) return false

    if (offset != other.offset) return false
    if (isMove != other.isMove) return false

    return true
  }

  override fun hashCode(): Int {
    var result = offset
    result = 31 * result + isMove.hashCode()
    return result
  }

  companion object {
    /**
     * Factory method used to create an appropriate range based on the range text
     *
     * @param str    The range text
     * @param offset Any offset specified after the range
     * @param move   True if the cursor should be moved to the line that the address evaluates to
     * @return The ranges appropriate to the text
     */
    fun createRangeAddresses(str: String, offset: Int, move: Boolean): Array<Address>? {
      // Current line
      if (str.isEmpty() || str == ".") {
        return arrayOf(CurrentLineAddress(offset, move))
      } else if (str == "%") {
        return arrayOf(
          LineAddress(1, 0, move),  // Remember, one-based!
          LastLineAddress(offset, move)
        )
      } else if (str == "$") {
        return arrayOf(LastLineAddress(offset, move))
      } else if (str.startsWith("'") && str.length == 2) {
        return arrayOf(MarkAddress(str[1], offset, move))
      } else if (str.startsWith("/") || str.startsWith("\\/") || str.startsWith("\\&")) {
        return arrayOf(SearchAddress(str, offset, move))
      } else if (str.startsWith("?") || str.startsWith("\\?")) {
        return arrayOf(SearchAddress(str, offset, move))
      } else {
        try {
          val line1 = str.toInt()
          return arrayOf(LineAddress(line1, offset, move))
        } catch (e: NumberFormatException) { // Ignore - we'll send back bad range later.
        }
      }
      // User entered an invalid range.
      return null
    }
  }
}

/**
 * Represents a specific line in the buffer
 *
 * @param line1   The one-based line of the address
 * @param offset  The offset added or removed to the line that the address evaluates to, e.g. `:1,'a+3`
 * @param move    True if the address should move the caret to the line that the address evaluates to
 */
@TestOnly
class LineAddress(private val line1: Int, offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine1(editor: VimEditor, caret: ImmutableVimCaret): Int = line1

  override fun equals(other: Any?): Boolean {
    return super.equals(other) && (other as? LineAddress)?.line1 == this.line1
  }

  override fun hashCode(): Int = super.hashCode() + 31 * line1
  override fun toString(): String = "LineAddress[line1=$line1, ${super.toString()}]"
}

/**
 * Represents the current line for the given caret
 *
 * Entered as `.` in the command line.
 */
private class CurrentLineAddress(offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine1(editor: VimEditor, caret: ImmutableVimCaret): Int {
    return caret.getBufferPosition().line + 1 // Convert zero-based line to one-based
  }

  override fun toString(): String = "CurrentLineAddress[${super.toString()}]"
}

/**
 * Represents the last line in the buffer
 *
 * Entered as `$` in the command line.
 */
private class LastLineAddress(offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine1(editor: VimEditor, caret: ImmutableVimCaret): Int {
    return editor.lineCount()
  }

  override fun toString(): String = "LastLineAddress[${super.toString()}]"
}

/**
 * Represents the line specified by a mark
 */
@TestOnly // Should be private. Constructor is visible for test purposes only
class MarkAddress(private val mark: Char, offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine1(editor: VimEditor, caret: ImmutableVimCaret): Int {
    val mark = injector.markService.getMark(caret, mark)
      ?: throw exExceptionMessage("E20")
    return mark.line + 1
  }

  override fun equals(other: Any?): Boolean {
    return super.equals(other) && (other as? MarkAddress)?.mark == this.mark
  }

  override fun hashCode(): Int = super.hashCode() + 31 * mark.hashCode()
  override fun toString(): String = "MarkAddress[mark=$mark, ${super.toString()}]"
}

/**
 * Represents a range given by a search pattern. The pattern can be '\\/', '\\?', '\\&amp;', /{pattern}/,
 * or ?{pattern}?.  The last two can be repeated zero or more times after any of the others.
 */
private class SearchAddress(pattern: String, offset: Int, move: Boolean) : Address(offset, move) {
  private companion object {
    private val logger = vimLogger<SearchAddress>()
  }

  private val patterns: MutableList<String?> = mutableListOf()
  private val directions: MutableList<Direction> = mutableListOf()

  init {
    logger.debug { "pattern=$pattern" }
    // Search range patterns such as `/one//two/` will be separated by a NULL character, rather than handled as separate
    // ranges. A range with an offset, such as `/one/+3/two/` will be treated as two ranges.
    val tok = StringTokenizer(pattern, "\u0000")
    while (tok.hasMoreTokens()) {
      var pat = tok.nextToken()
      when (pat) {
        "\\/" -> {
          patterns.add(injector.searchGroup.lastSearchPattern)
          directions.add(Direction.FORWARDS)
        }

        "\\?" -> {
          patterns.add(injector.searchGroup.lastSearchPattern)
          directions.add(Direction.BACKWARDS)
        }

        "\\&" -> {
          patterns.add(injector.searchGroup.lastSubstitutePattern)
          directions.add(Direction.FORWARDS)
        }

        else -> {
          if (pat[0] == '/') {
            directions.add(Direction.FORWARDS)
          } else {
            directions.add(Direction.BACKWARDS)
          }
          pat = if (pat.last() == pat[0]) {
            pat.substring(1, pat.length - 1)
          } else {
            pat.substring(1)
          }
          patterns.add(pat)
        }
      }
    }
  }

  override fun calculateLine1(
    editor: VimEditor,
    caret: ImmutableVimCaret,
  ): Int {
    var line0 = caret.getBufferPosition().line
    var searchOffset: Int
    for (i in patterns.indices) {
      val pattern = patterns[i]
      val direction = directions[i]

      // TODO: Handling of line offset is kinda hacky
      // We pass it in, but don't apply it to the search result. It should only be applied to the last pattern, and so
      // is applied by the base class in getLine. But we need to pass it into processSearchRange so that
      // lastPatternOffset is updated for future searches
      // Perhaps we should handle offset as part of the search, so we don't have to coerce to avoid the E16 validation?
      val patternOffset = if (i == patterns.size - 1) offset else 0

      // Note that wrapscan, ignorecase, etc. all come from current option values, as expected
      searchOffset = getSearchOffset(editor, line0, direction)
      searchOffset = injector.searchGroup.processSearchRange(editor, pattern!!, patternOffset, searchOffset, direction)

      if (searchOffset == -1) {
        if (injector.options(editor).wrapscan) {
          throw exExceptionMessage("E486", pattern)
        } else {
          throw exExceptionMessage("E385", pattern)
        }
      }
      line0 = editor.offsetToBufferPosition(searchOffset).line
    }
    return line0 + 1
  }

  private fun getSearchOffset(editor: VimEditor, line: Int, direction: Direction): Int {
    return if (direction == Direction.FORWARDS) {
      injector.motion.moveCaretToLineEnd(editor, line, true)
    } else {
      injector.motion.moveCaretToLineStart(editor, line)
    }
  }

  // Search ranges do not report "E16: Invalid range" with a too large negative offset, but the other ranges do.
  // It's like the other ranges do validation - they know what the range's line is without any work, so validation is
  // quick. Search has to do some work, so it's not validated, and silently coerced. Messy, but it's Vim compatibility.
  // Alternatively, offset could be applied and coerced as part of the search, so it's valid when it comes out
  override fun applyOffset(line: Int): Int {
    return when {
      line != -1 -> (line + offset).coerceAtLeast(0)
      else -> line
    }
  }

  override fun equals(other: Any?): Boolean {
    return super.equals(other) && (other as? SearchAddress)?.patterns == this.patterns
  }

  override fun hashCode(): Int = super.hashCode() + 31 * patterns.hashCode()
  override fun toString(): String = "SearchAddress[patterns=$patterns, ${super.toString()}]"
}
