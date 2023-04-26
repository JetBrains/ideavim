/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import kotlin.math.abs
import kotlin.math.min

// todo all this methods should return Long since editor.fileSize is long
// todo same for TextRange and motions
// However, editor.text() returns a CharSequence, which can only be indexed by Int
public abstract class VimSearchHelperBase : VimSearchHelper {
  public companion object {
    private val logger = vimLogger<VimSearchHelperBase>()
  }

  override fun findNextWord(
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    return doFindNext(editor, searchFrom, count, bigWord, spaceWords, ::findNextWordOne)
  }

  override fun findNextWordEnd(
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    return doFindNext(editor, searchFrom, count, bigWord, spaceWords, ::findNextWordEndOne)
  }

  private fun doFindNext(
    editor: VimEditor,
    searchFrom: Int,
    countDirection: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
    action: (VimEditor, pos: Int, size: Int, step: Int, bigWord: Boolean, spaceWords: Boolean) -> Int
  ): Int {
    var count = countDirection
    val step = if (count >= 0) 1 else -1
    count = abs(count)
    val size = editor.fileSize().toInt()  // editor.text() returns CharSequence, which only supports Int indexing
    var pos = searchFrom
    for (i in 0 until count) {
      pos = action(editor, pos, size, step, bigWord, spaceWords)
      if (pos == searchFrom || pos == 0 || pos == size - 1) {
        break
      }
    }
    return pos
  }

  private fun findNextWordOne(
    editor: VimEditor,
    pos: Int,
    size: Int,
    step: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    val chars = editor.text()
    var found = false
    var _pos = if (pos < size) pos else min(size, (chars.length - 1))
    // For back searches, skip any current whitespace so we start at the end of a word
    if (step < 0 && _pos > 0) {
      if (charType(chars[_pos - 1], bigWord) === CharacterHelper.CharacterType.WHITESPACE && !spaceWords) {
        _pos = skipSpace(chars, _pos - 1, step, size) + 1
      }
      if (_pos > 0 && charType(chars[_pos], bigWord) !== charType(chars[_pos - 1], bigWord)) {
        _pos += step
      }
    }
    var res = _pos
    if (_pos < 0 || _pos >= size) {
      return _pos
    }
    var type = charType(chars[_pos], bigWord)
    if (type === CharacterHelper.CharacterType.WHITESPACE && step < 0 && _pos > 0 && !spaceWords) {
      type = charType(chars[_pos - 1], bigWord)
    }
    _pos += step
    while (_pos in 0 until size && !found) {
      val newType = charType(chars[_pos], bigWord)
      if (newType !== type) {
        if (newType === CharacterHelper.CharacterType.WHITESPACE && step >= 0 && !spaceWords) {
          _pos = skipSpace(chars, _pos, step, size)
          res = _pos
        } else if (step < 0) {
          res = _pos + 1
        } else {
          res = _pos
        }
        type = charType(chars[res], bigWord)
        found = true
      }
      _pos += step
    }
    if (found) {
      if (res < 0) { // (pos <= 0)
        res = 0
      } else if (res >= size) { // (pos >= size)
        res = size - 1
      }
    } else if (_pos <= 0) {
      res = 0
    } else if (_pos >= size) {
      res = size
    }
    return res
  }

  private fun findNextWordEndOne(
    editor: VimEditor,
    pos: Int,
    size: Int,
    step: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    val chars = editor.text()
    var pos = pos
    var found = false
    // For forward searches, skip any current whitespace so we start at the start of a word
    if (step > 0 && pos < size - 1) {
      if (charType(chars[pos + 1], bigWord) === CharacterHelper.CharacterType.WHITESPACE &&
        !spaceWords
      ) {
        pos = skipSpace(chars, pos + 1, step, size) - 1
      }
      if (pos < size - 1 &&
        charType(chars[pos], bigWord) !==
        charType(chars[pos + 1], bigWord)
      ) {
        pos += step
      }
    }
    var res = pos
    if (pos < 0 || pos >= size) {
      return pos
    }
    var type = charType(chars[pos], bigWord)
    if (type === CharacterHelper.CharacterType.WHITESPACE && step >= 0 && pos < size - 1 && !spaceWords) {
      type = charType(chars[pos + 1], bigWord)
    }
    pos += step
    while (pos >= 0 && pos < size && !found) {
      val newType = charType(chars[pos], bigWord)
      if (newType !== type) {
        if (step >= 0) {
          res = pos - 1
        } else if (newType === CharacterHelper.CharacterType.WHITESPACE && step < 0 && !spaceWords) {
          pos = skipSpace(chars, pos, step, size)
          res = pos
        } else {
          res = pos
        }
        found = true
      }
      pos += step
    }
    if (found) {
      if (res < 0) {
        res = 0
      } else if (res >= size) {
        res = size - 1
      }
    } else if (pos == size) {
      res = size - 1
    }
    return res
  }

  private fun skipSpace(chars: CharSequence, offset: Int, step: Int, size: Int): Int {
    var _offset = offset
    var prev = 0.toChar()
    while (_offset in 0 until size) {
      val c = chars[_offset]
      if (c == '\n' && c == prev) break
      if (charType(c, false) !== CharacterHelper.CharacterType.WHITESPACE) break
      prev = c
      _offset += step
    }
    return if (_offset < size) _offset else size - 1
  }

  override fun findNextCamelStart(chars: CharSequence, startIndex: Int, count: Int): Int? {
    return findCamelStart(chars, startIndex, count, Direction.FORWARDS)
  }

  override fun findPreviousCamelStart(chars: CharSequence, endIndex: Int, count: Int): Int? {
    return findCamelStart(chars, endIndex - 1, count, Direction.BACKWARDS)
  }

  override fun findNextCamelEnd(chars: CharSequence, startIndex: Int, count: Int): Int? {
    return findCamelEnd(chars, startIndex, count, Direction.FORWARDS)
  }

  override fun findPreviousCamelEnd(chars: CharSequence, endIndex: Int, count: Int): Int? {
    return findCamelEnd(chars, endIndex - 1, count, Direction.BACKWARDS)
  }

  /**
   * [startIndex] is inclusive
   */
  private fun findCamelStart(chars: CharSequence, startIndex: Int, count: Int, direction: Direction): Int? {
    assert(count >= 1)
    var counter = 0
    var offset = startIndex
    while (counter < count) {
      val searchFrom = if (counter == 0) offset else offset + direction.toInt()
      offset = findCamelStart(chars, searchFrom, direction) ?: return null
      ++counter
    }
    return offset
  }

  /**
   * [startIndex] is inclusive
   */
  private fun findCamelEnd(chars: CharSequence, startIndex: Int, count: Int, direction: Direction): Int? {
    assert(count >= 1)
    var counter = 0
    var offset = startIndex
    while (counter < count) {
      val searchFrom = if (counter == 0) offset else offset + direction.toInt()
      offset = findCamelEnd(chars, searchFrom, direction) ?: return null
      ++counter
    }
    return offset
  }

  /**
   * [startIndex] is inclusive
   */
  private fun findCamelStart(chars: CharSequence, startIndex: Int, direction: Direction): Int? {
    var pos = startIndex
    val size = chars.length

    if (pos < 0 || pos >= size) {
      return null
    }

    while (pos in 0 until size) {
      if (chars[pos].isUpperCase()) {
        if ((pos == 0 || !chars[pos - 1].isUpperCase()) ||
          (pos == size - 1 || chars[pos + 1].isLowerCase())
        ) {
          return pos
        }
      } else if (chars[pos].isLowerCase()) {
        if (pos == 0 || !chars[pos - 1].isLetter()) {
          return pos
        }
      } else if (chars[pos].isDigit()) {
        if (pos == 0 || !chars[pos - 1].isDigit()) {
          return pos
        }
      }
      pos += direction.toInt()
    }
    return null
  }

  /**
   * [startIndex] is inclusive
   */
  private fun findCamelEnd(chars: CharSequence, startIndex: Int, direction: Direction): Int? {
    var pos = startIndex
    val size = chars.length

    if (pos < 0 || pos >= size) {
      return pos
    }

    while (pos in 0 until size) {
      if (chars[pos].isUpperCase()) {
        if (pos == size - 1 || !chars[pos + 1].isLetter() ||
          (chars[pos + 1].isUpperCase() && pos < size - 2 && chars[pos + 2].isLowerCase())
        ) {
          return pos
        }
      } else if (chars[pos].isLowerCase()) {
        if (pos == size - 1 || !chars[pos + 1].isLowerCase()) {
          return pos
        }
      } else if (chars[pos].isDigit()) {
        if (pos == size - 1 || !chars[pos + 1].isDigit()) {
          return pos
        }
      }
      pos += direction.toInt()
    }
    return null
  }

  override fun findBlockQuoteInLineRange(editor: VimEditor, caret: ImmutableVimCaret, quote: Char, isOuter: Boolean): TextRange? {
    var leftQuote: Int
    var rightQuote: Int

    val caretOffset = caret.offset.point
    val quoteAfterCaret: Int = editor.text().indexOfNext(quote, caretOffset, true) ?: return null
    val quoteBeforeCaret: Int? = editor.text().indexOfPrevious(quote, caretOffset, true)
    val quotesBeforeCaret: Int = editor.text().occurrencesBeforeOffset(quote, caretOffset, true)

    if (((caretOffset == quoteAfterCaret) && quotesBeforeCaret % 2 == 0) || quoteBeforeCaret == null) {
      leftQuote = quoteAfterCaret
      rightQuote = editor.text().indexOfNext(quote, leftQuote + 1, true) ?: return null
    } else {
      leftQuote = quoteBeforeCaret
      rightQuote = quoteAfterCaret
    }
    if (!isOuter) {
      leftQuote++
      rightQuote--
    }
    return TextRange(leftQuote, rightQuote + 1)
  }

  /**
   * @param endOffset         right search border, it is not included in search
   */
  private fun CharSequence.occurrencesBeforeOffset(char: Char, endOffset: Int, currentLineOnly: Boolean, searchEscaped: Boolean = false): Int {
    var counter = 0
    var i = endOffset
    while (i > 0 && this[i + Direction.BACKWARDS.toInt()] != '\n') {
      i = this.indexOfPrevious(char, i, currentLineOnly, searchEscaped) ?: break
      counter++
    }
    return counter
  }

  /**
   * @param char              char to look for
   * @param startIndex        index to start the search from, included in search
   * @param currentLineOnly   true if search should stop after reaching '\n'
   * @param searchEscaped     true if escaped chars should appear in search
   *
   * @return the closest to [startIndex] position of [char], or null if no [char] was found
   */
  private fun CharSequence.indexOfNext(char: Char, startIndex: Int, currentLineOnly: Boolean, searchEscaped: Boolean = false): Int? {
    return findCharacterPosition(this, char, startIndex, Direction.FORWARDS, currentLineOnly, searchEscaped)
  }

  /**
   * @param char              char to look for
   * @param endIndex          right search border, it is not included in search
   * @param currentLineOnly   true if search should stop after reaching '\n'
   * @param searchEscaped     true if escaped chars should appear in search
   *
   * @return the closest to [endIndex] position of [char], or null if no [char] was found
   */
  private fun CharSequence.indexOfPrevious(char: Char, endIndex: Int, currentLineOnly: Boolean, searchEscaped: Boolean = false): Int? {
    if (endIndex == 0 || (currentLineOnly && this[endIndex - 1] == '\n')) return null
    return findCharacterPosition(this, char, endIndex - 1, Direction.BACKWARDS, currentLineOnly, searchEscaped)
  }

  /**
   * Gets the closest char position in the given direction
   *
   * @param startIndex        index to start the search from (included in search)
   * @param char              character to look for
   * @param currentLineOnly   true if search should break after reaching '\n'
   * @param searchEscaped     true if escaped chars should be returned
   * @param direction         direction to search (forward/backward)
   *
   * @return index of the closest char found (null if nothing was found)
   */
  private fun findCharacterPosition(charSequence: CharSequence, char: Char, startIndex: Int, direction: Direction, currentLineOnly: Boolean, searchEscaped: Boolean): Int? {
    var pos = startIndex
    while (pos >= 0 && pos < charSequence.length && (!currentLineOnly || charSequence[pos] != '\n')) {
      if (charSequence[pos] == char && (pos == 0 || searchEscaped || isQuoteWithoutEscape(charSequence, pos, char))) {
        return pos
      }
      pos += direction.toInt()
    }
    return null
  }

  /**
   * Returns true if [quote] is at this [position] and it's not escaped (like \")
   */
  private fun isQuoteWithoutEscape(chars: CharSequence, position: Int, quote: Char): Boolean {
    var i = position
    if (chars[i] != quote) return false
    var backslashCounter = 0
    while (i-- > 0 && chars[i] == '\\') {
      backslashCounter++
    }
    return backslashCounter % 2 == 0
  }

  override fun findNextParagraph(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowBlanks: Boolean): Int? {
    val line: Int = findNextParagraphLine(editor, caret.getBufferPosition().line, count, allowBlanks) ?: return null
    val lineCount: Int = editor.nativeLineCount()
    return if (line == lineCount - 1) {
      if (count > 0) editor.fileSize().toInt() - 1 else 0
    } else {
      editor.getLineStartOffset(line)
    }
  }

  /**
   * Find next paragraph bound offset
   * @param editor target editor
   * @param startLine line to start the search from (included in search)
   * @param direction search direction
   * @param allowBlanks true if we consider lines with whitespaces as empty
   * @return next paragraph offset
   */
  private fun findNextParagraph(editor: VimEditor, startLine: Int, direction: Direction, allowBlanks: Boolean): Int {
    val line: Int? = findNextParagraphLine(editor, startLine, direction, allowBlanks)
    return if (line == null) {
      if (direction == Direction.FORWARDS) editor.fileSize().toInt() - 1 else 0
    } else {
      editor.getLineStartOffset(line)
    }
  }

  override fun findParagraphRange(editor: VimEditor, caret: ImmutableVimCaret, count: Int, isOuter: Boolean): TextRange? {
    val line: Int = caret.getBufferPosition().line

    if (logger.isDebug()) {
      logger.debug("Starting paragraph range search on line $line")
    }

    val rangeInfo = (if (isOuter) findOuterParagraphRange(editor, line, count) else findInnerParagraphRange(editor, line, count)) ?: return null
    val startLine: Int = rangeInfo.first
    val endLine: Int = rangeInfo.second

    if (logger.isDebug()) {
      logger.debug("final start line= $startLine")
      logger.debug("final end line= $endLine")
    }

    val start: Int = editor.getLineStartOffset(startLine)
    val end: Int = editor.getLineStartOffset(endLine)
    return TextRange(start, end + 1)
  }

  private fun findOuterParagraphRange(editor: VimEditor, line: Int, count: Int): Pair<Int, Int>? {
    var expandStart = false
    var expandEnd = false

    var startLine: Int = if (editor.isLineEmpty(line, true)) {
      line
    } else {
      findNextParagraphLine(editor, line, -1, true) ?: return null
    }

    var endLine: Int = findNextParagraphLine(editor, line, count, true) ?: return null

    if (editor.isLineEmpty(startLine, true) && editor.isLineEmpty(endLine, true)) {
      if (startLine == line) {
        endLine--
        expandStart = true
      } else {
        startLine++
        expandEnd = true
      }
    } else if (!editor.isLineEmpty(endLine, true) && !editor.isLineEmpty(startLine, true) && startLine > 0) {
      startLine--
      expandStart = true
    } else {
      expandStart = editor.isLineEmpty(startLine, true)
      expandEnd = editor.isLineEmpty(endLine, true)
    }
    if (expandStart) {
      startLine = if (editor.isLineEmpty(startLine, true)) findLastEmptyLine(editor, startLine, Direction.BACKWARDS) else startLine
    }
    if (expandEnd) {
      endLine = if (editor.isLineEmpty(endLine, true)) findLastEmptyLine(editor, endLine, Direction.FORWARDS) else endLine
    }
    return Pair(startLine, endLine)
  }

  private fun findInnerParagraphRange(editor: VimEditor, line: Int, count: Int): Pair<Int, Int>? {
    val lineCount: Int = editor.lineCount()

    var startLine = line
    var endLine: Int

    if (!editor.isLineEmpty(startLine, true)) {
      startLine = findNextParagraphLine(editor, line, -1, true) ?: return null
      if (editor.isLineEmpty(startLine, true)) {
        startLine++
      }
      endLine = line
    } else {
      endLine = line - 1
    }
    // todo someone please refactor this if you understand what is going on
    var which = if (editor.isLineEmpty(startLine, true)) 0 else 1
    for (i in 0 until count) {
      if (which % 2 == 1) {
        val nextParagraphLine = findNextParagraphLine(editor, endLine, Direction.FORWARDS, true)
        endLine = if (nextParagraphLine == null || nextParagraphLine == 0) {
          if (i == count - 1) {
            lineCount - 1
          } else {
            return null
          }
        } else {
          nextParagraphLine - 1
        }
      } else {
        endLine++
      }
      which++
    }
    startLine = if (editor.isLineEmpty(startLine, true)) findLastEmptyLine(editor, startLine, Direction.BACKWARDS) else startLine
    endLine = if (editor.isLineEmpty(endLine, true)) findLastEmptyLine(editor, endLine, Direction.FORWARDS) else endLine
    return Pair(startLine, endLine)
  }

  /**
   * If we have multiple consecutive empty lines in an editor, the method returns the first
   * or last empty line in the group of empty lines, depending on the specified direction
   */
  private fun findLastEmptyLine(editor: VimEditor, line: Int, direction: Direction): Int {
    if (!editor.isLineEmpty(line, true)) {
      logger.error("Method findLastEmptyLine was called for non-empty line")
      return line
    }
    return if (direction == Direction.BACKWARDS) {
      val previousNonEmptyLine = skipEmptyLines(editor, line, Direction.BACKWARDS, true)
      previousNonEmptyLine + 1
    } else {
      val nextNonEmptyLine = skipEmptyLines(editor, line, Direction.FORWARDS, true)
      nextNonEmptyLine - 1
    }
  }
  /**
   * Find next paragraph bound line
   * @param editor target editor
   * @param startLine line to start the search from (included in search)
   * @param count search for the count-th occurrence
   * @param allowBlanks true if we consider lines with whitespaces as empty
   * @return next paragraph bound line if there is any
   */
  private fun findNextParagraphLine(editor: VimEditor, startLine: Int, count: Int, allowBlanks: Boolean): Int? {
    var line: Int? = startLine
    val lineCount: Int = editor.lineCount()
    val direction = if (count > 0) Direction.FORWARDS else Direction.BACKWARDS

    var i = abs(count)
    while (i > 0 && line != null) {
      line = findNextParagraphLine(editor, line, direction, allowBlanks)
      i--
    }

    if (count != 0 && i == 0 && line == null) {
      line = if (direction == Direction.FORWARDS) lineCount - 1 else 0
    }

    return line
  }

  /**
   * Searches for the next paragraph boundary (empty line)
   * @param editor target editor
   * @param startLine line to start the search from (included in search)
   * @param direction search direction
   * @param allowBlanks true if we consider lines with whitespaces as empty
   * @return next empty line if there is any
   */
  private fun findNextParagraphLine(editor: VimEditor, startLine: Int, direction: Direction, allowBlanks: Boolean): Int? {
    var line = skipEmptyLines(editor, startLine, direction, allowBlanks)
    while (line in 0 until editor.nativeLineCount()) {
      if (editor.isLineEmpty(line, allowBlanks)) return line
      line += direction.toInt()
    }
    return null
  }

  /**
   * Searches for the next non-empty line in the given direction
   * @param editor target editor
   * @param startLine line to start the search from (included in search)
   * @param direction search direction
   * @param allowBlanks true if we consider lines with whitespaces as empty
   * @return next non-empty line if there is any.
   * If there is no such line, the [editor.nativeLineCount()] is returned for Forwards direction and -1 for Backwards
   */
  private fun skipEmptyLines(editor: VimEditor, startLine: Int, direction: Direction, allowBlanks: Boolean): Int {
    var i = startLine
    while (i in 0 until editor.nativeLineCount()) {
      if (!editor.isLineEmpty(i, allowBlanks)) break
      i += direction.toInt()
    }
    return i
  }
}
