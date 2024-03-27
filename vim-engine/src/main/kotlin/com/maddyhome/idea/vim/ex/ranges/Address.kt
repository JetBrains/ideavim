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
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
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
 * See `:help range` and `:help {address}`.
 *
 * @param offset  The relative offset added or subtracted from the evaluated line number
 * @param isMove  True if the caret should be moved after evaluating this address. In the text representation, a
 *                semicolon follows this address.
 */
public sealed class Address(public val offset: Int, public val isMove: Boolean) {
  /**
   * Gets the zero-based line number specified by this address.
   *
   * If the range included an offset (`+1`, `-1`), this is applied to the returned line number.
   *
   * Note that the user will have used one-based line numbers, but internally, we use zero-based. This conversion is
   * automatically handled.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if the last line was set to start of file
   * @return The zero-based line number or -1 if unable to get the line number
   */
  public fun getLine(editor: VimEditor, lastZero: Boolean): Int {
    // TODO: Only apply offset if calculateLine returns a valid line number
    val line = calculateLine(editor, lastZero)
    return line + offset
  }

  /**
   * Gets the zero-based line number specified by this address.
   *
   * If the range included an offset (`+1`, `-1`), this is applied to the returned line number.
   *
   * Note that the user will have used one-based line numbers, but internally, we use zero-based. This conversion is
   * automatically handled.
   *
   * @param editor   The editor to get the line for
   * @param caret    The caret to use for the current line or initial search line, if required
   * @param lastZero True if the last line was set to start of file
   * @return The zero-based line number or -1 if unable to get the line number
   */
  public fun getLine(editor: VimEditor, caret: ImmutableVimCaret, lastZero: Boolean): Int {
    // TODO: Why does this not pass through caret?
    // TODO: Only apply offset if calculateLine returns a valid line number
    return if (offset == 0) calculateLine(editor, lastZero) else calculateLine(editor, caret, lastZero) + offset
  }

  override fun toString(): String = "Range{offset=$offset, move=$isMove}"

  /**
   * Calculate the line number specified by this address. Does not apply offset
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if the last line was set to start of file
   * @return The zero-based line number, -1 if unable to get the line number
   */
  protected abstract fun calculateLine(editor: VimEditor, lastZero: Boolean): Int

  /**
   * Calculate the line number specified by this address. Does not apply offset
   *
   * @param editor   The editor to get the line for
   * @param caret    The caret to use for initial search offset, or to get the current line, etc.
   * @param lastZero True if the last line was set to start of file
   * @return The zero-based line number, -1 if unable to get the line number
   */
  protected abstract fun calculateLine(editor: VimEditor, caret: ImmutableVimCaret, lastZero: Boolean): Int

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

  public companion object {
    /**
     * Factory method used to create an appropriate range based on the range text
     *
     * @param str    The range text
     * @param offset Any offset specified after the range
     * @param move   True if the cursor should be moved to the line that the address evaluates to
     * @return The ranges appropriate to the text
     */
    public fun createRangeAddresses(str: String, offset: Int, move: Boolean): Array<Address>? {
      // Current line
      if (str.isEmpty() || str == ".") {
        return arrayOf(CurrentLineAddress(offset, move))
      } else if (str == "%") {
        return arrayOf(
          LineAddress(0, 0, move),
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
          val line = str.toInt() - 1  // Convert to 0-based line
          return arrayOf(LineAddress(line, offset, move))
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
 * @param line    The zero-based line of the address
 * @param offset  The offset added or removed to the line that the address evaluates to, e.g. `:1,'a+3`
 * @param move    True if the address should move the caret to the line that the address evaluates to
 */
@TestOnly
public class LineAddress(private val line: Int, offset: Int, move: Boolean) : Address(offset, move) {

  override fun calculateLine(editor: VimEditor, lastZero: Boolean): Int = line
  override fun calculateLine(editor: VimEditor, caret: ImmutableVimCaret, lastZero: Boolean): Int = line

  override fun equals(other: Any?): Boolean {
    return super.equals(other) && (other as? LineAddress)?.line == this.line
  }

  override fun hashCode(): Int = super.hashCode() + 31 * line
  override fun toString(): String = "LineAddress[line=$line, ${super.toString()}]"
}

/**
 * Represents the current line for the given caret
 *
 * Entered as `.` in the command line.
 */
private class CurrentLineAddress(offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine(editor: VimEditor, lastZero: Boolean): Int {
    return calculateLine(editor, editor.primaryCaret(), lastZero)
  }

  override fun calculateLine(editor: VimEditor, caret: ImmutableVimCaret, lastZero: Boolean): Int {
    return caret.getBufferPosition().line
  }

  override fun toString(): String = "CurrentLineAddress[${super.toString()}]"
}

/**
 * Represents the last line in the buffer
 *
 * Entered as `$` in the command line.
 */
private class LastLineAddress(offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine(editor: VimEditor, lastZero: Boolean): Int {
    return calculateLine(editor, editor.primaryCaret(), lastZero)
  }

  override fun calculateLine(editor: VimEditor, caret: ImmutableVimCaret, lastZero: Boolean): Int {
    return editor.lineCount() - 1
  }

  override fun toString(): String = "LastLineAddress[${super.toString()}]"
}

/**
 * Represents the line specified by a mark
 */
@TestOnly // Should be private. Constructor is visible for test purposes only
public class MarkAddress(private val mark: Char, offset: Int, move: Boolean) : Address(offset, move) {
  override fun calculateLine(editor: VimEditor, lastZero: Boolean): Int {
    return injector.markService.getMark(editor.currentCaret(), this.mark)?.line ?: -1
  }

  override fun calculateLine(editor: VimEditor, caret: ImmutableVimCaret, lastZero: Boolean): Int {
    // TODO: Why is this not passing through the caret?
    return calculateLine(editor, lastZero)
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

  override fun calculateLine(
    editor: VimEditor,
    lastZero: Boolean,
  ): Int {
    // Each subsequent pattern is searched for starting in the line after the previous search match
    return calculateLine(editor, editor.currentCaret(), lastZero)
  }

  override fun calculateLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
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

      // Note that wrapscan, ignorecase, etc. all come from current option values, as expected
      searchOffset = getSearchOffset(editor, line, direction, lastZero)
      searchOffset = injector.searchGroup.processSearchRange(editor, pattern!!, patternOffset, searchOffset, direction)
      // TODO: Vim throws E385 if it can't find a result and wrapscan isn't set
      // TODO: Vim throws E486 if it can't find a result with wrapscan set - IdeaVim does the same
      if (searchOffset == -1) break
      line = editor.offsetToBufferPosition(searchOffset).line
    }
    return if (searchOffset != -1) line else -1
  }

  private fun getSearchOffset(editor: VimEditor, line: Int, direction: Direction, lastZero: Boolean): Int {
    // TODO: I'm not sure this is correct
    // lastZero is true if we have an address that evaluates to less than 0. I'm not sure of the circumstances when this
    // is expected to be true. It can be true if there are no matches to search, or for something like `1-20` (first
    // line, with an offset of minus 20). This would mean that the next search starts at the beginning of the "current"
    // line, rather than the following line.
    // This leads to behaviour such as `/foo/-20/foo/d` to delete the first line (assuming 'foo' is in the first line),
    // which doesn't work in Vim.
    // Firstly, we should only return a negative value for an error, which would mean that lastZero is only set when the
    // last address cannot be resolved (cannot find search or no defined mark)
    return if (direction == Direction.FORWARDS && !lastZero) {
      injector.motion.moveCaretToLineEnd(editor, line, true)
    } else {
      injector.motion.moveCaretToLineStart(editor, line)
    }
  }

  override fun equals(other: Any?): Boolean {
    return super.equals(other) && (other as? SearchAddress)?.patterns == this.patterns
  }

  override fun hashCode(): Int = super.hashCode() + 31 * patterns.hashCode()
  override fun toString(): String = "SearchAddress[patterns=$patterns, ${super.toString()}]"
}
