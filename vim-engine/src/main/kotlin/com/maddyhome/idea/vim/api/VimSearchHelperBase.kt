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
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import kotlin.math.abs
import kotlin.math.min

public abstract class VimSearchHelperBase : VimSearchHelper {
  override fun findNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Long {
    return findNextWord(editor.text(), searchFrom.toLong(), editor.fileSize(), count, bigWord, false)
  }

  override fun findNextWordEnd(editor: VimEditor, caret: ImmutableVimCaret, count: Int, bigWord: Boolean): Int {
    val chars = editor.text()
    val pos = caret.offset.point
    val size = editor.fileSize().toInt()
    return findNextWordEnd(chars, pos, size, count, bigWord, false)
  }

  override fun findNextWordEnd(
    chars: CharSequence,
    pos: Int,
    size: Int,
    count: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    return VimSearchHelperBase.findNextWordEnd(chars, pos, size, count, bigWord, spaceWords)
  }

  public companion object {
    public fun findNextWord(
      chars: CharSequence,
      pos: Long,
      size: Long,
      count: Int,
      bigWord: Boolean,
      spaceWords: Boolean,
    ): Long {
      var _count = count
      val step = if (_count >= 0) 1 else -1
      _count = abs(_count)
      var res = pos
      for (i in 0 until _count) {
        res = findNextWordOne(chars, res, size, step, bigWord, spaceWords)
        if (res == pos || res == 0L || res == size - 1) {
          break
        }
      }
      return res
    }

    // TODO: 18.08.2022 Make private
    public fun findNextWordOne(
      chars: CharSequence,
      pos: Long,
      size: Long,
      step: Int,
      bigWord: Boolean,
      spaceWords: Boolean,
    ): Long {
      var found = false
      var _pos = if (pos < size) pos else min(size, (chars.length - 1).toLong())
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

    /**
     * Skip whitespace starting with the supplied position.
     *
     * An empty line is considered a whitespace break.
     */
    // TODO: 18.08.2022 Make private
    public fun skipSpace(chars: CharSequence, offset: Long, step: Int, size: Long): Long {
      var _offset = offset
      var prev = 0.toChar()
      while (_offset in 0 until size) {
        val c = chars[_offset.toInt()]
        if (c == '\n' && c == prev) break
        if (charType(c, false) !== CharacterHelper.CharacterType.WHITESPACE) break
        prev = c
        _offset += step
      }
      return if (_offset < size) _offset else size - 1
    }

    public operator fun CharSequence.get(index: Long): Char {
      return this[index.toInt()]
    }

    public fun findNextWordEndOne(
      chars: CharSequence,
      pos: Int,
      size: Int,
      step: Int,
      bigWord: Boolean,
      spaceWords: Boolean,
    ): Int {
      var pos = pos
      var found = false
      // For forward searches, skip any current whitespace so we start at the start of a word
      if (step > 0 && pos < size - 1) {
        if (charType(chars[pos + 1], bigWord) === CharacterHelper.CharacterType.WHITESPACE &&
          !spaceWords
        ) {
          pos = (skipSpace(chars, (pos + 1).toLong(), step, size.toLong()) - 1).toInt()
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
            pos = skipSpace(chars, pos.toLong(), step, size.toLong()).toInt()
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
    public fun findNextWordEnd(
      chars: CharSequence,
      pos: Int,
      size: Int,
      count: Int,
      bigWord: Boolean,
      spaceWords: Boolean,
    ): Int {
      var count = count
      val step = if (count >= 0) 1 else -1
      count = abs(count)
      var res = pos
      for (i in 0 until count) {
        res = findNextWordEndOne(chars, res, size, step, bigWord, spaceWords)
        if (res == pos || res == 0 || res == size - 1) {
          break
        }
      }
      return res
    }
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
}
