/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.lion

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

/**
 * The lines an alignment works on, optionally limited to columns for characterwise and blockwise ranges.
 *
 * Columns are 0-based. The start columns are where the search for the alignment pattern begins, the end columns are
 * exclusive limits of the searched text, `null` meaning the whole line.
 */
internal data class LionRange(
  val startLine: Int,
  val endLine: Int,
  private val firstLineStartColumn: Int = 0,
  private val middleStartColumn: Int = 0,
  private val lastLineEndColumn: Int? = null,
  private val middleEndColumn: Int? = null,
) {
  val lines: IntRange
    get() = startLine..endLine

  fun searchStart(line: Int): Int = if (line == startLine) firstLineStartColumn else middleStartColumn

  fun searchEnd(line: Int, lineLength: Int): Int {
    val end = if (line == endLine) lastLineEndColumn else middleEndColumn
    return end?.coerceAtMost(lineLength) ?: lineLength
  }

  companion object {
    fun lineWise(startLine: Int, endLine: Int): LionRange = LionRange(startLine, endLine)

    /** [end] is inclusive */
    fun characterWise(start: BufferPosition, end: BufferPosition): LionRange =
      LionRange(start.line, end.line, firstLineStartColumn = start.column, lastLineEndColumn = end.column + 1)

    /** [endColumn] is inclusive, `null` when the block extends to the end of each line */
    fun blockWise(startLine: Int, endLine: Int, startColumn: Int, endColumn: Int?): LionRange {
      val end = endColumn?.plus(1)
      return LionRange(startLine, endLine, startColumn, startColumn, end, end)
    }
  }
}

/**
 * What the text is aligned to.
 *
 * A match is tried at a single column, like a Vim pattern match attempt, and yields the column the match starts at.
 */
internal sealed interface LionPattern {
  /**
   * Tries to match at [column] of [line], whose text is [lineText], without going past [endColumn] (exclusive).
   *
   * @return the column where the match starts, or `null` if there is no match at [column]
   */
  fun matchAt(editor: VimEditor, line: Int, lineText: String, column: Int, endColumn: Int): Int?
}

/**
 * Matches [text] literally and case-sensitively.
 *
 * With [matchEnd], the match position is right after the text instead of at its start.
 */
internal class LiteralLionPattern(private val text: String, private val matchEnd: Boolean) : LionPattern {
  override fun matchAt(editor: VimEditor, line: Int, lineText: String, column: Int, endColumn: Int): Int? {
    if (column + text.length > endColumn || !lineText.startsWith(text, column)) return null
    return if (matchEnd) column + text.length else column
  }
}

/**
 * Matches a Vim regular expression, respecting 'ignorecase'.
 *
 * With [matchEnd], the match position is right after the matched text, unless the pattern sets it with `\zs`.
 *
 * @throws com.maddyhome.idea.vim.regexp.VimRegexException if [pattern] is not a valid pattern
 */
internal class RegexLionPattern(pattern: String, matchEnd: Boolean) : LionPattern {
  private val regex = VimRegex(if (matchEnd && !pattern.contains("\\zs")) "$pattern\\zs" else pattern)

  override fun matchAt(editor: VimEditor, line: Int, lineText: String, column: Int, endColumn: Int): Int? {
    val lineStart = editor.getLineStartOffset(line)
    val options =
      if (injector.globalOptions().ignorecase) enumSetOf(VimRegexOptions.IGNORE_CASE) else noneOfEnum()
    val match = regex.matchAt(editor, lineStart + column, options) as? VimMatchResult.Success ?: return null
    if (match.range.endOffset > lineStart + endColumn) return null
    return match.range.startOffset - lineStart
  }
}

/**
 * Aligns the lines of [range] by inserting spaces, so that the matches of [pattern] end up in the same screen column.
 *
 * With [alignAfter], the text following each match is aligned instead of the match itself.
 */
internal class LionAligner(
  private val editor: VimEditor,
  private val range: LionRange,
  private val pattern: LionPattern,
  private val alignAfter: Boolean,
  private val tabSize: Int,
) {
  /**
   * Aligns the matches one occurrence at a time, starting from occurrence `skip + 1`.
   *
   * @param count how many occurrences to align, 0 to align all of them
   * @param skip  how many leading occurrences to leave as they are
   */
  fun align(count: Int, skip: Int) {
    var iteration = 1
    while (iteration <= MAX_ITERATIONS) {
      val positions = range.lines.associateWith { findPosition(it, skip + iteration) }
      val widths = positions.mapValues { (line, column) ->
        if (column == -1) -1 else displayWidth(editor.getLineText(line), column)
      }
      val longest = widths.values.maxOrNull() ?: -1
      if (longest == -1) break

      for ((line, column) in positions) {
        val width = widths.getValue(line)
        if (width != -1 && width < longest) {
          val offset = editor.getLineStartOffset(line) + column
          injector.changeGroup.replaceText(editor, editor.primaryCaret(), offset, offset, " ".repeat(longest - width))
        }
      }

      if (iteration == count) break
      iteration++
    }
  }

  /** The column of the [nth] match of [pattern] on [line], or -1 */
  private fun findPosition(line: Int, nth: Int): Int {
    val text = editor.getLineText(line)
    val end = range.searchEnd(line, text.length)
    var column = range.searchStart(line)
    var remaining = nth
    while (column <= end) {
      val match = (column..end).firstNotNullOfOrNull { pattern.matchAt(editor, line, text, it, end) } ?: return -1
      remaining--
      if (remaining == 0) return if (alignAfter) firstNonBlank(text, match, end) else match
      // The next attempt starts one character after the start of this match
      column = match + 1
    }
    return -1
  }

  private fun firstNonBlank(text: String, from: Int, end: Int): Int =
    (from until end).firstOrNull { !isBlank(text[it]) } ?: -1

  private fun displayWidth(text: String, column: Int): Int {
    var width = 0
    for (i in 0 until column) {
      width += if (text[i] == '\t') tabSize - (width % tabSize) else 1
    }
    return width
  }

  companion object {
    /**
     * A safety net for patterns whose matches multiply as spaces are inserted, such as a pattern matching a space
     */
    private const val MAX_ITERATIONS = 1000

    private fun isBlank(c: Char): Boolean = c == ' ' || c == '\t'

    /**
     * Collapses every run of two or more spaces into a single space, except in the line's indent
     */
    fun squeezeSpaces(text: String): String {
      val indent = text.indexOfFirst { !isBlank(it) }.takeIf { it != -1 } ?: return text
      return text.substring(0, indent) + text.substring(indent).replace(MULTIPLE_SPACES, " ")
    }

    private val MULTIPLE_SPACES = Regex(" {2,}")
  }
}
