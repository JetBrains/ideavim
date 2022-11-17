/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex.ranges

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.util.*

/**
 * Base for all Ex command ranges
 */
sealed class Range(
  // Line offset
  protected val offset: Int,
  val isMove: Boolean,
) {
  /**
   * Gets the line number (0 based) specificied by this range. Includes the offset.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if unable to get the line number
   */
  fun getLine(editor: VimEditor, lastZero: Boolean): Int {
    val line = getRangeLine(editor, lastZero)
    return line + offset
  }

  fun getLine(editor: VimEditor, caret: VimCaret, lastZero: Boolean): Int {
    return if (offset == 0) getRangeLine(editor, lastZero) else getRangeLine(editor, caret, lastZero) + offset
  }

  override fun toString(): String = "Range{offset=$offset, move=$isMove}"

  /**
   * Gets the line number specified by this range without regard to any offset.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if inable to get the line number
   */
  protected abstract fun getRangeLine(editor: VimEditor, lastZero: Boolean): Int

  protected abstract fun getRangeLine(editor: VimEditor, caret: VimCaret, lastZero: Boolean): Int

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Range) return false

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
     * @param move   True if cursor should be moved to range line
     * @return The ranges appropriate to the text
     */
    @JvmStatic
    fun createRange(str: String, offset: Int, move: Boolean): Array<Range>? {
      // Current line
      if (str.isEmpty() || str == ".") {
        return arrayOf(LineNumberRange(offset, move))
      } else if (str == "%") {
        return arrayOf(
          LineNumberRange(0, 0, move),
          LineNumberRange(LineNumberRange.LAST_LINE, offset, move)
        )
      } else if (str == "$") {
        return arrayOf(LineNumberRange(LineNumberRange.LAST_LINE, offset, move))
      } else if (str.startsWith("'") && str.length == 2) {
        return arrayOf(MarkRange(str[1], offset, move))
      } else if (str.startsWith("/") || str.startsWith("\\/") || str.startsWith("\\&")) {
        return arrayOf(SearchRange(str, offset, move))
      } else if (str.startsWith("?") || str.startsWith("\\?")) {
        return arrayOf(SearchRange(str, offset, move))
      } else {
        try {
          val line = str.toInt() - 1
          return arrayOf(LineNumberRange(line, offset, move))
        } catch (e: NumberFormatException) { // Ignore - we'll send back bad range later.
        }
      }
      // User entered an invalid range.
      return null
    }
  }
}

/**
 * Represents a specific line, the current line, or the last line of a file
 */
class LineNumberRange : Range {
  /**
   * Create a range for the current line
   *
   * @param offset The range offset
   * @param move   True if cursor should be moved
   */
  constructor(offset: Int, move: Boolean) : super(offset, move) {
    line = CURRENT_LINE
  }

  /**
   * Create a range for the given line
   *
   * @param offset The range offset
   * @param move   True if cursor should be moved
   */
  constructor(line: Int, offset: Int, move: Boolean) : super(offset, move) {
    this.line = line
  }

  /**
   * Gets the line number specified by this range without regard to any offset.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 for start of file
   */
  override fun getRangeLine(editor: VimEditor, lastZero: Boolean): Int {
    if (line == CURRENT_LINE) {
      line = editor.currentCaret().getBufferPosition().line
    } else if (line == LAST_LINE) {
      line = editor.lineCount() - 1
    }
    return line
  }

  override fun getRangeLine(
    editor: VimEditor,
    caret: VimCaret,
    lastZero: Boolean,
  ): Int {
    line = if (line == LAST_LINE) editor.lineCount() - 1 else caret.getBufferPosition().line
    return line
  }

  override fun toString(): String = "LineNumberRange[line=$line, ${super.toString()}]"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LineNumberRange

    if (line != other.line) return false
    if (offset != other.offset) return false
    if (isMove != other.isMove) return false

    return true
  }

  override fun hashCode(): Int {
    val prime = 31
    return isMove.hashCode() + prime * offset.hashCode() + prime * prime * line.hashCode()
  }

  private var line: Int

  companion object {
    const val CURRENT_LINE = -99999999
    const val LAST_LINE = -99999998
  }
}

/**
 * Represents the line specified by a mark
 */
class MarkRange(private val mark: Char, offset: Int, move: Boolean) : Range(offset, move) {
  /**
   * Gets the line number specified by this range without regard to any offset.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if there is no such mark set in the file
   */
  override fun getRangeLine(editor: VimEditor, lastZero: Boolean): Int {
    val mark = injector.markGroup.getFileMark(editor, mark)
    return mark?.logicalLine ?: -1
  }

  override fun getRangeLine(editor: VimEditor, caret: VimCaret, lastZero: Boolean): Int = getRangeLine(editor, lastZero)

  override fun toString(): String = "MarkRange[mark=$mark, ${super.toString()}]"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MarkRange

    if (mark != other.mark) return false
    if (offset != other.offset) return false
    if (isMove != other.isMove) return false

    return true
  }

  override fun hashCode(): Int {
    val prime = 31
    return prime * prime * mark.hashCode() + prime * offset.hashCode() + isMove.hashCode()
  }
}

/**
 * Represents a range given by a search pattern. The pattern can be '\\/', '\\?', '\\&amp;', /{pattern}/,
 * or ?{pattern}?.  The last two can be repeated 0 or more times after any of the others.
 */
class SearchRange(pattern: String, offset: Int, move: Boolean) : Range(offset, move) {
  /**
   * Parses the pattern into a list of subpatterns and flags
   *
   * @param pattern The full search pattern
   */
  private fun setPattern(pattern: String) {
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

  /**
   * Gets the line number specified by this range without regard to any offset.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if the text was not found
   */
  override fun getRangeLine(
    editor: VimEditor,
    lastZero: Boolean,
  ): Int { // Each subsequent pattern is searched for starting in the line after the previous search match
    return getRangeLine(editor, editor.currentCaret(), lastZero)
  }

  override fun getRangeLine(
    editor: VimEditor,
    caret: VimCaret,
    lastZero: Boolean,
  ): Int {
    var line = caret.getBufferPosition().line
    var searchOffset = -1
    for (i in patterns.indices) {
      val pattern = patterns[i]
      val direction = directions[i]

      // TODO: Handling of line offset is kinda hacky
      // We pass it in, but don't apply it to the search result. It should only be applied to the last pattern, and so
      // is applied by the base class in getLine. But we need to pass it into processSearchRange so that
      // lastPatternOffset is updated for future searches
      val patternOffset = if (i == patterns.size - 1) offset else 0

      searchOffset = getSearchOffset(editor, line, direction, lastZero)
      searchOffset = injector.searchGroup.processSearchRange(editor, pattern!!, patternOffset, searchOffset, direction)
      if (searchOffset == -1) break
      line = editor.offsetToBufferPosition(searchOffset).line
    }
    return if (searchOffset != -1) line else -1
  }

  private fun getSearchOffset(editor: VimEditor, line: Int, direction: Direction, lastZero: Boolean): Int {
    return if (direction == Direction.FORWARDS && !lastZero) {
      injector.motion.moveCaretToLineEnd(editor, line, true)
    } else {
      injector.motion.moveCaretToLineStart(editor, line)
    }
  }

  override fun toString(): String = "SearchRange[patterns=$patterns, ${super.toString()}]"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SearchRange

    if (patterns != other.patterns) return false
    if (directions != other.directions) return false
    if (offset != other.offset) return false
    if (isMove != other.isMove) return false

    return true
  }

  override fun hashCode(): Int {
    var result = patterns.hashCode()
    result = 31 * result + directions.hashCode()
    result = 31 * result + offset.hashCode()
    result = 31 * result + isMove.hashCode()
    return result
  }

  private val patterns: MutableList<String?> = mutableListOf()
  private val directions: MutableList<Direction> = mutableListOf()

  companion object {
    private val logger = vimLogger<SearchRange>()
  }

  init {
    setPattern(pattern)
  }
}
