/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.ex.ranges

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * Base for all Ex command ranges
 */
sealed class Range(
  // Line offset
  protected val offset: Int,
  val isMove: Boolean
) {
  /**
   * Gets the line number (0 based) specificied by this range. Includes the offset.
   *
   * @param editor   The editor to get the line for
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if unable to get the line number
   */
  fun getLine(editor: Editor, lastZero: Boolean): Int {
    val line = getRangeLine(editor, lastZero)
    return line + offset
  }

  fun getLine(editor: Editor, caret: Caret, lastZero: Boolean): Int {
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
  protected abstract fun getRangeLine(editor: Editor, lastZero: Boolean): Int

  protected abstract fun getRangeLine(editor: Editor, caret: Caret, lastZero: Boolean): Int

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
  override fun getRangeLine(editor: Editor, lastZero: Boolean): Int {
    if (line == CURRENT_LINE) {
      line = editor.caretModel.logicalPosition.line
    } else if (line == LAST_LINE) {
      line = EditorHelper.getLineCount(editor) - 1
    }
    return line
  }

  override fun getRangeLine(
    editor: Editor, caret: Caret,
    lastZero: Boolean
  ): Int {
    line = if (line == LAST_LINE) EditorHelper.getLineCount(editor) - 1 else caret.logicalPosition.line
    return line
  }

  override fun toString(): String = "LineNumberRange[line=$line, ${super.toString()}]"

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
  override fun getRangeLine(editor: Editor, lastZero: Boolean): Int {
    val mark = VimPlugin.getMark().getFileMark(editor, mark)
    return mark?.logicalLine ?: -1
  }

  override fun getRangeLine(editor: Editor, caret: Caret, lastZero: Boolean): Int = getRangeLine(editor, lastZero)

  override fun toString(): String = "MarkRange[mark=$mark, ${super.toString()}]"

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
    val tok = StringTokenizer(pattern, "\u0000")
    while (tok.hasMoreTokens()) {
      var pat = tok.nextToken()
      when (pat) {
        "\\/" -> {
          patterns.add(VimPlugin.getSearch().lastSearch)
          flags.add(enumSetOf(CommandFlags.FLAG_SEARCH_FWD))
        }
        "\\?" -> {
          patterns.add(VimPlugin.getSearch().lastSearch)
          flags.add(enumSetOf(CommandFlags.FLAG_SEARCH_REV))
        }
        "\\&" -> {
          patterns.add(VimPlugin.getSearch().lastPattern)
          flags.add(enumSetOf(CommandFlags.FLAG_SEARCH_FWD))
        }
        else -> {
          if (pat[0] == '/') {
            flags.add(enumSetOf(CommandFlags.FLAG_SEARCH_FWD))
          } else {
            flags.add(enumSetOf(CommandFlags.FLAG_SEARCH_REV))
          }
          pat = pat.substring(1)
          if (pat.last() == pat[0]) {
            pat = pat.substring(0, pat.length - 1)
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
    editor: Editor,
    lastZero: Boolean
  ): Int { // Each subsequent pattern is searched for starting in the line after the previous search match
    var line = editor.caretModel.logicalPosition.line
    var pos = -1
    for (i in patterns.indices) {
      val pattern = patterns[i]
      val flag = flags[i]
      pos = if (CommandFlags.FLAG_SEARCH_FWD in flag && !lastZero) {
        VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true)
      } else {
        VimPlugin.getMotion().moveCaretToLineStart(editor, line)
      }
      pos = VimPlugin.getSearch().search(editor, pattern!!, pos, 1, flag)
      line = if (pos == -1) {
        break
      } else {
        editor.offsetToLogicalPosition(pos).line
      }
    }
    return if (pos != -1) line else -1
  }

  override fun getRangeLine(
    editor: Editor, caret: Caret,
    lastZero: Boolean
  ): Int {
    var line = caret.logicalPosition.line
    var offset = -1
    for (i in patterns.indices) {
      val pattern = patterns[i]
      val flag = flags[i]
      offset = VimPlugin.getSearch().search(editor, pattern!!, getSearchOffset(editor, line, flag, lastZero), 1, flag)
      if (offset == -1) break
      line = editor.offsetToLogicalPosition(offset).line
    }
    return if (offset != -1) line else -1
  }

  private fun getSearchOffset(editor: Editor, line: Int, flag: EnumSet<CommandFlags>, lastZero: Boolean): Int {
    return if (flag.contains(CommandFlags.FLAG_SEARCH_FWD) && !lastZero) {
      VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true)
    } else VimPlugin.getMotion().moveCaretToLineStart(editor, line)
  }

  override fun toString(): String = "SearchRange[patterns=$patterns, ${super.toString()}]"

  private val patterns: MutableList<String?> = mutableListOf()
  private val flags: MutableList<EnumSet<CommandFlags>> = mutableListOf()

  companion object {
    private val logger = logger<SearchRange>()
  }

  init {
    setPattern(pattern)
  }
}
