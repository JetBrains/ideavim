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
import com.maddyhome.idea.vim.helper.CharacterHelper.isWhitespace
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Range
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

// todo all this methods should return Long since editor.fileSize is long
// todo same for TextRange and motions
// However, editor.text() returns a CharSequence, which can only be indexed by Int
abstract class VimSearchHelperBase : VimSearchHelper {
  companion object {
    private val logger = vimLogger<VimSearchHelperBase>()
  }

  override fun findSection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    direction: Int,
    count: Int,
  )
    : Int {
    val documentText: CharSequence = editor.text()
    var currentLine: Int = caret.getBufferPosition().line + direction
    var resultOffset = -1
    var remainingTargets = count

    while (currentLine in 1 until editor.lineCount() && remainingTargets > 0) {
      val lineStartOffset = editor.getLineStartOffset(currentLine)
      if (lineStartOffset < documentText.length) {
        val currentChar = documentText[lineStartOffset]
        if (currentChar == type || currentChar == '\u000C') {
          resultOffset = lineStartOffset
          remainingTargets--
        }
      }
      currentLine += direction
    }

    if (resultOffset == -1) {
      resultOffset = if (direction < 0) 0 else documentText.length - 1
    }

    return resultOffset
  }

  override fun findNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int {
    val line: Int = caret.getBufferPosition().line
    val start = editor.getLineStartOffset(line)
    val end = editor.getLineEndOffset(line, true)
    val chars: CharSequence = editor.text()
    var found = 0
    val step = if (count >= 0) 1 else -1
    var pos: Int = caret.offset + step
    while (pos in start until end && pos < chars.length) {
      if (chars[pos] == ch) {
        found++
        if (found == abs(count)) {
          break
        }
      }
      pos += step
    }

    return if (found == abs(count)) {
      pos
    } else {
      -1
    }
  }

  override fun findNextWord(
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
  ): Int {
    return findNextWord(editor.text(), editor, searchFrom, count, bigWord)
  }

  override fun findNextWord(
    text: CharSequence,
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
  ): Int {
    var pos = searchFrom
    repeat(abs(count)) {
      pos = if (count > 0) {
        findNextWordOne(text, editor, pos, bigWord)
      } else {
        findPreviousWordOne(text, editor, pos, bigWord)
      }

      if (pos >= text.length) return pos
    }
    return pos
  }

  override fun findNextWordEnd(
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
    stopOnEmptyLine: Boolean,
    allowMoveFromWordEnd: Boolean
  ): Int {
    val text = editor.text()
    var pos = searchFrom
    repeat(abs(count)) {
      pos = if (count > 0) {
        findNextWordEndOne(text, editor, pos, bigWord, stopOnEmptyLine, allowMoveFromWordEnd)
      } else {
        findPreviousWordEndOne(text, editor, pos, bigWord)
      }
    }
    return pos
  }

  override fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange? {
    if (pattern.isNullOrEmpty()) return null

    val dir = if (searchOptions!!.contains(SearchOptions.BACKWARDS)) Direction.BACKWARDS else Direction.FORWARDS

    val options = enumSetOf<VimRegexOptions>()
    if (injector.globalOptions().smartcase && !searchOptions.contains(SearchOptions.IGNORE_SMARTCASE)) options.add(
      VimRegexOptions.SMART_CASE
    )
    if (injector.globalOptions().ignorecase) options.add(VimRegexOptions.IGNORE_CASE)
    if (searchOptions.contains(SearchOptions.WANT_ENDPOS)) {
      // When we want to get the end position of a search match, we can match at the current location. Having these as
      // separate flags means we can remove CAN_MATCH_START_LOCATION for subsequent matches (i.e., count)
      options.add(VimRegexOptions.WANT_END_POSITION)
      options.add(VimRegexOptions.CAN_MATCH_START_LOCATION)
    }

    val wrap = searchOptions.contains(SearchOptions.WRAP)
    val showMessages = searchOptions.contains(SearchOptions.SHOW_MESSAGES)

    val regex = try {
      VimRegex(pattern)
    } catch (e: VimRegexException) {
      injector.messages.showStatusBarMessage(editor, e.message)
      return null
    }

    var result = if (dir === Direction.FORWARDS) {
      findNextWithWrapscan(editor, regex, startOffset, options, wrap, showMessages)
    } else {
      findPreviousWithWrapscan(editor, regex, startOffset, options, wrap, showMessages)
    }

    if (result is VimMatchResult.Failure) {
      if (wrap) {
        // E486: Pattern not found {0}
        injector.messages.showStatusBarMessage(editor, injector.messages.message("E486", pattern))
      } else if (dir === Direction.FORWARDS) {
        // E385: Search hit BOTTOM without match for: {0}
        injector.messages.showStatusBarMessage(editor, injector.messages.message(Msg.E385, pattern))
      } else {
        // E385: Search hit TOP without match for: {0}
        injector.messages.showStatusBarMessage(editor, injector.messages.message(Msg.E384, pattern))
      }
      return null
    }

    // When trying to find the end position for a match, we're allowed to match the current position. But if we do that
    // on subsequent matches when we have a count, then we'll get stuck at the current location. Remove the flag.
    options.remove(VimRegexOptions.CAN_MATCH_START_LOCATION)
    for (i in 1 until count) {
      val nextOffset = (result as VimMatchResult.Success).range.startOffset
      result =
        if (dir === Direction.FORWARDS) {
          findNextWithWrapscan(editor, regex, nextOffset, options, wrap, showMessages)
        } else {
          findPreviousWithWrapscan(editor, regex, nextOffset, options, wrap, showMessages)
        }
      if (result is VimMatchResult.Failure) {
        // We know this isn't pattern not found...
        if (searchOptions.contains(SearchOptions.SHOW_MESSAGES)) {
          if (dir === Direction.FORWARDS) {
            // E385: Search hit BOTTOM without match for: {0}
            injector.messages.showStatusBarMessage(editor, injector.messages.message(Msg.E385, pattern))
          } else {
            // E385: Search hit TOP without match for: {0}
            injector.messages.showStatusBarMessage(editor, injector.messages.message(Msg.E384, pattern))
          }
        }
        return null
      }
    }

    return (result as VimMatchResult.Success).range
  }

  private fun findNextWithWrapscan(
    editor: VimEditor,
    regex: VimRegex,
    startIndex: Int,
    options: EnumSet<VimRegexOptions>,
    wrapscan: Boolean,
    showMessages: Boolean,
  ): VimMatchResult {
    val result = regex.findNext(editor, startIndex, options)
    if (result is VimMatchResult.Failure && wrapscan) {
      if (showMessages) {
        // search hit BOTTOM, continuing at TOP
        injector.messages.showStatusBarMessage(editor, injector.messages.message("message.search.hit.bottom"))
      }
      // Start searching from the start of the file, but accept a match at the start offset
      val newOptions = options.clone().also { it.add(VimRegexOptions.CAN_MATCH_START_LOCATION) }
      return regex.findNext(editor, 0, newOptions)
    }
    return result
  }

  private fun findPreviousWithWrapscan(
    editor: VimEditor,
    regex: VimRegex,
    startIndex: Int,
    options: EnumSet<VimRegexOptions>,
    wrapscan: Boolean,
    showMessages: Boolean,
  ): VimMatchResult {
    val result = regex.findPrevious(editor, startIndex, options)
    if (result is VimMatchResult.Failure && wrapscan) {
      if (showMessages) {
        // search hit TOP, continuing at BOTTOM
        injector.messages.showStatusBarMessage(editor, injector.messages.message("message.search.hit.top"))
      }
      return regex.findPrevious(editor, editor.fileSize().toInt() - 1, options)
    }
    return result
  }

  override fun findAll(
    editor: VimEditor,
    pattern: String,
    startLine: Int,
    endLine: Int,
    ignoreCase: Boolean,
  ): List<TextRange> {
    val options = enumSetOf<VimRegexOptions>()
    if (injector.globalOptions().smartcase) options.add(VimRegexOptions.SMART_CASE)
    if (injector.globalOptions().ignorecase) options.add(VimRegexOptions.IGNORE_CASE)
    val regex = try {
      VimRegex(pattern)
    } catch (e: VimRegexException) {
      injector.messages.showStatusBarMessage(editor, e.message)
      return emptyList()
    }
    return regex.findAll(
      editor,
      editor.getLineStartOffset(startLine),
      editor.getLineEndOffset(if (endLine == -1) editor.lineCount() - 1 else endLine) + 1,
      options
    ).map { it.range }
  }

  /**
   * Find the next word from the current starting position, skipping current word and whitespace
   *
   * Note that this will return an out of bound index if there is no next word! This is necessary to distinguish between
   * no next word and the next word being on the last character of the file.
   *
   * Also remember that two different "word" types can butt up against each other - e.g. KEYWORD followed by PUNCTUATION
   */
  private fun findNextWordOne(
    chars: CharSequence,
    editor: VimEditor,
    start: Int,
    bigWord: Boolean,
    stopAtEndOfLine: Boolean = false,
  ): Int {
    var pos = start
    if (pos >= chars.length) return chars.length

    val startingCharType = charType(editor, chars[start], bigWord)

    // It is important to move first, to properly handle stopping at the end of a line and moving from an empty line
    pos++

    // If we're on a word, move past the end of it
    if (pos < chars.length && startingCharType != CharacterHelper.CharacterType.WHITESPACE) {
      pos = skipWhileCharacterType(editor, chars, pos, 1, startingCharType, bigWord)
    }

    // Skip following whitespace, optionally stopping at the end of the line (on the newline char).
    // An empty line is a word, so stop when the offset is at the newline char of an empty line.
    while (pos < chars.length && isWhitespace(editor, chars[pos], bigWord)) {
      if (isEmptyLine(chars, pos) || (chars[pos] == '\n' && stopAtEndOfLine)) return pos
      pos++
    }

    // We're now on the first character of a word or just past the end of the file
    return pos.coerceAtMost(chars.length)
  }

  /**
   * Find the start of the current word, skipping current whitespace
   *
   * This function will always return an in-bounds index. If there is no previous word (because we're at the start of
   * the file), the offset will be `0`, the start of the current word or preceding whitespace.
   */
  private fun findPreviousWordOne(
    chars: CharSequence,
    editor: VimEditor,
    start: Int,
    bigWord: Boolean,
  ): Int {
    var pos = start

    // Always move back one to make sure that we don't get stuck on the start of a word
    pos--

    // Skip any intermediate whitespace, stopping at an empty line (offset is the newline char of the empty line).
    // This will leave us on the last character of the previous word.
    while (pos >= 0 && isWhitespace(editor, chars[pos], bigWord)) {
      if (isEmptyLine(chars, pos)) return pos
      pos--
    }

    // We're now on a word character, or at the start of the file. Move back until we're past the start of the word,
    // then move forward to the start of the word
    if (pos >= 0) {
      pos = skipWhileCharacterType(editor, chars, pos, -1, charType(editor, chars[pos], bigWord), bigWord) + 1
    }

    return pos.coerceAtLeast(0)
  }

  // TODO: Remove this once findWordUnderCursor has been properly rewritten
  private fun oldFindNextWordOne(
    chars: CharSequence,
    editor: VimEditor,
    pos: Int,
    size: Int,
    step: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    var found = false
    var _pos = pos  // CAREFUL! This might be at the end of the file, but we need this for calculations below

    // For back searches, skip any current whitespace so we start at the end of a word
    if (step < 0 && _pos > 0) {
      if (charType(editor, chars[_pos - 1], bigWord) === CharacterHelper.CharacterType.WHITESPACE && !spaceWords) {
        _pos = skipSpace(editor, chars, pos - 1, step, size, true) + 1
      }
      // _pos might be at the end of file. Handle this so we don't try to walk backwards based on incorrect char type
      if (_pos == size || (_pos > 0 && charType(editor, chars[_pos], bigWord) !== charType(editor, chars[_pos - 1], bigWord))) {
        _pos += step
      }
    }
    var res = _pos.coerceAtMost(size - 1)
    if (_pos < 0 || _pos >= size) {
      return _pos
    }
    var char = chars[_pos]
    var lineLength = 0
    var type = charType(editor, char, bigWord)
    if (type === CharacterHelper.CharacterType.WHITESPACE && step < 0 && _pos > 0 && !spaceWords) {
      type = charType(editor, chars[_pos - 1], bigWord)
    }
    _pos += step
    while (_pos in 0 until size && !found) {
      val newChar = chars[_pos]
      val newType = charType(editor, chars[_pos], bigWord)
      if (newType !== type) {
        if (newType === CharacterHelper.CharacterType.WHITESPACE && step >= 0 && !spaceWords) {
          _pos = skipSpace(editor, chars, _pos, step, size, true)
          res = _pos
        } else if (step < 0) {
          res = _pos + 1
        } else {
          res = _pos
        }
        type = charType(editor, chars[res], bigWord)
        found = true
      } else if (newChar == '\n' && (spaceWords || lineLength == 0)) {
        // An empty line is considered a word/WORD, and if we're matching spaces as words, new line is a terminator
        res = if (step < 0) _pos + 1 else _pos
        found = true
      }

      if (newChar == '\n') lineLength = 0 else lineLength++

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

  @Suppress("GrazieInspection")
  private fun findNextWordEndOne(
    chars: CharSequence,
    editor: VimEditor,
    start: Int,
    bigWord: Boolean,
    stopOnEmptyLine: Boolean,
    allowMoveFromWordEnd: Boolean,
  ): Int {
    // Scenarios:
    // * Cursor on word/WORD: move to end of the current word/WORD group ("wo${c}rd    word" -> "wor${c}d    word")
    // * Cursor on end of word/WORD: move to end of next word/WORD group ("wor${c}d    word" -> "word    wor${c}d")
    // * Cursor on whitespace: move to end of next word/WORD group       (" ${c}       word" -> "        wor${c}d")
    // * Visual on single char:
    //   For `e`/`E`: move to end of next word/WORD group ("   ${s}d${se}    word" -> "   ${s}d    word${se}")
    //   For `iw` and no selection: DO NOT MOVE!          ("   ${s}d${se}    word" -> "   ${s}d${se}    word")
    //
    // Note that newline characters are considered to be whitespace. This is USUALLY treated like whitespace, so
    // moving through a new line to get to next word end is usually just the same as moving through other whitespace.
    // Empty lines are an exception to this rule. Vim considers an empty line to be a word/WORD, but for vi
    // compatibility reasons, `e` and `E` do not stop on empty lines, but text objects do.

    var pos = start
    val startingCharType = charType(editor, chars[pos], bigWord)

    // If we process the loops below, we end up one character past the target offset. But we don't always process the
    // loops. Move one character forward now so everything works cleanly
    pos++

    // When called by `e`/`E` and we're originally at the end of the current word, we want to move to end of the *next*
    // word, so skip whitespace at the current pos and get to the start of the next word.
    // When called by `iw` and we're at the end of the current word, we don't want to move, so if there's whitespace at
    // the current position, don't skip it. We then won't be at the start of the next word, so we won't move further.
    // We'll decrement before we return, so the cursor doesn't move.
    // But if we start on whitespace, we always want to skip it, for both scenarios.
    if (allowMoveFromWordEnd || startingCharType == CharacterHelper.CharacterType.WHITESPACE) {
      while (pos < chars.length && isWhitespace(editor, chars[pos], bigWord)) {
        if (stopOnEmptyLine && isEmptyLine(chars, pos)) return pos
        pos++
      }
    }

    // If we're on whitespace now, it's because we started on the end of a word and then immediately advanced to
    // whitespace. We don't want to skip the current whitespace (as above) and we don't want to
    if (pos < chars.length && !isWhitespace(editor, chars[pos], bigWord)) {
      // We're currently at the start of, or inside, a word/WORD. Move to the start of the next charType segment, and
      // return the character *before* it, so we get the end of the current word/WORD
      val wordCharType = charType(editor, chars[pos], bigWord)
      pos = skipWhileCharacterType(editor, chars, pos, 1, wordCharType, bigWord)
    }

    // The loops above have put us one char past the next word, or one past whitespace. Or just advanced one character
    pos--

    return pos.coerceIn(0, chars.length - 1)
  }

  private fun findPreviousWordEndOne(
    chars: CharSequence,
    editor: VimEditor,
    start: Int,
    bigWord: Boolean,
  ): Int {
    var pos = start
    val startingCharType = charType(editor, chars[pos], bigWord)

    // We always have to move backwards
    pos--

    // Find the first preceding character with a different charType. Note that we use the starting charType here,
    // because we might have already moved across a word/WORD/non-word boundary.
    // We don't need to +/-1 here. This is returning the offset of the character at the end of the previous word/WORD
    if (pos >= 0 && startingCharType != CharacterHelper.CharacterType.WHITESPACE) {
      pos = skipWhileCharacterType(editor, chars, pos, -1, startingCharType, bigWord)
    }

    // If we ended up on whitespace, skip backwards until we find either the last character of the previous word/WORD,
    // or we have to stop at an empty line, which is considered a WORD
    while (pos in 0 until chars.length && isWhitespace(editor, chars[pos], bigWord)) {
      // Always check empty line when moving backwards
      if (isEmptyLine(chars, pos)) return pos
      pos--
    }

    return pos.coerceAtLeast(0)
  }

  private fun skipSpace(
    editor: VimEditor,
    chars: CharSequence,
    offset: Int,
    step: Int,
    size: Int,
    matchEmptyLine: Boolean,
  ): Int {
    var _offset = offset
    var prev = 0.toChar()
    while (_offset in 0 until size) {
      val c = chars[_offset]
      if (c == '\n' && c == prev && matchEmptyLine) break
      if (charType(editor, c, false) !== CharacterHelper.CharacterType.WHITESPACE) break
      prev = c
      _offset += step
    }
    return if (_offset < size) _offset else size - 1
  }

  private fun isEmptyLine(chars: CharSequence, offset: Int): Boolean {
    // The new line char belongs to the current line, so if the previous character is also a new line char, then the
    // line for the current offset is empty
    return (offset == 0 && chars[offset] == '\n')
      || (chars[offset] == '\n' && chars[offset - 1] == '\n')
  }

  /**
   * Skips characters in a given direction until reaching a different character type
   *
   * Returns the offset of the character with the different character type. Will return indexes out of bounds at the
   * start and end of the file. Specifically, will return `-1` at the start of the file, `chars.length` at the end.
   */
  private fun skipWhileCharacterType(
    editor: VimEditor,
    chars: CharSequence,
    start: Int,
    step: Int,
    type: CharacterHelper.CharacterType,
    isBig: Boolean,
  ): Int {
    var offset = start
    while (offset in 0 until chars.length && charType(editor, chars[offset], isBig) === type) {
      offset += step
    }
    return offset
  }

  private fun skipOneCharacterBack(offset: Int): Int {
    return (offset - 1).coerceAtLeast(0)
  }

  private fun skipOneCharacterBackOnCurrentLine(chars: CharSequence, offset: Int): Int {
    return if (chars[offset - 1] != '\n') offset - 1 else offset
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

  override fun findBlockQuoteInLineRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    quote: Char,
    isOuter: Boolean,
  ): TextRange? {
    var leftQuote: Int
    var rightQuote: Int

    val caretOffset = caret.offset
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
  private fun CharSequence.occurrencesBeforeOffset(
    char: Char,
    endOffset: Int,
    currentLineOnly: Boolean,
    searchEscaped: Boolean = false,
  ): Int {
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
  private fun CharSequence.indexOfNext(
    char: Char,
    startIndex: Int,
    currentLineOnly: Boolean,
    searchEscaped: Boolean = false,
  ): Int? {
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
  private fun CharSequence.indexOfPrevious(
    char: Char,
    endIndex: Int,
    currentLineOnly: Boolean,
    searchEscaped: Boolean = false,
  ): Int? {
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
  private fun findCharacterPosition(
    charSequence: CharSequence,
    char: Char,
    startIndex: Int,
    direction: Direction,
    currentLineOnly: Boolean,
    searchEscaped: Boolean,
  ): Int? {
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

  @Contract(pure = true)
  override fun findNextSentenceStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    var count = count
    val dir = if (count > 0) Direction.FORWARDS else Direction.BACKWARDS
    count = Math.abs(count)
    val total = count
    val chars: CharSequence = editor.text()
    val start: Int = caret.offset
    val max: Int = editor.fileSize().toInt()
    var res: Int? = start
    while (count > 0 && res != null && res >= 0 && res <= max - 1) {
      res = findSentenceStart(editor, chars, res, max, dir, countCurrent)
      if (res == 0 || res == max - 1) {
        count--
        break
      }
      count--
    }
    if (res == null && (!requireAll || total == 1)) {
      res = if (dir == Direction.FORWARDS) max - 1 else 0
    } else if (count > 0 && total > 1 && !requireAll) {
      res = if (dir == Direction.FORWARDS) max - 1 else 0
    } else if (count > 0 && total > 1 && requireAll) {
      res = null
    }
    return res
  }

  @Contract(pure = true)
  override fun findNextSentenceEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    var count = count
    val dir = if (count > 0) Direction.FORWARDS else Direction.BACKWARDS
    count = Math.abs(count)
    val total = count
    val chars: CharSequence = editor.text()
    val start: Int = caret.offset
    val max: Int = editor.fileSize().toInt()
    var res: Int? = start
    while (count > 0 && res != null && res >= 0 && res <= max - 1) {
      res = findSentenceEnd(editor, chars, res, max, dir, countCurrent && count == total)
      if (res == 0 || res == max - 1) {
        count--
        break
      }
      count--
    }
    if (res == null && (!requireAll || total == 1)) {
      res = if (dir == Direction.FORWARDS) max - 1 else 0
    } else if (count > 0 && total > 1 && !requireAll) {
      res = if (dir == Direction.FORWARDS) max - 1 else 0
    } else if (count > 0 && total > 1 && requireAll) {
      res = null
    }
    return res
  }

  @Contract(pure = true)
  private fun findSentenceStart(
    editor: VimEditor,
    chars: CharSequence,
    start: Int,
    max: Int,
    dir: Direction,
    countCurrent: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    // Save off the next paragraph since a paragraph is a valid sentence.
    val lline: Int = editor.offsetToBufferPosition(start).line
    val np: Int = findNextParagraph(editor, lline, dir, false)
    var end: Int?
    // start < max was added to avoid exception and it may be incorrect
    end = if (start < max && chars[start] == '\n' && !countCurrent) {
      findSentenceEnd(editor, chars, start, max, Direction.BACKWARDS, false)
    } else {
      findSentenceEnd(editor, chars, start, max, Direction.BACKWARDS, true)
    }
    if (end == start && countCurrent && chars[end] == '\n') {
      return end
    }
    if (end != null && end >= 0) {
      var offset = end + 1
      while (offset < max) {
        val ch = chars[offset]
        if (!Character.isWhitespace(ch)) {
          break
        }
        offset++
      }
      if (dir == Direction.FORWARDS) {
        if (offset == start && countCurrent) {
          return offset
        } else if (offset > start) {
          return offset
        }
      } else {
        if (offset == start && countCurrent) {
          return offset
        } else if (offset < start) {
          return offset
        }
      }
    }
    end = if (dir == Direction.FORWARDS) {
      findSentenceEnd(editor, chars, start, max, dir, true)
    } else {
      end?.let { findSentenceEnd(editor, chars, it - 1, max, dir, countCurrent) }
    }
    var res: Int? = null
    if (end != null && end < chars.length && (chars[end] != '\n' || !countCurrent)) {
      res = end + 1
      while (res < max) {
        val ch = chars[res]
        if (!Character.isWhitespace(ch)) {
          break
        }
        res++
      }
    }

    // Now let's see which to return, the sentence we found or the paragraph we found.
    // This mess returns which ever is closer to our starting point (and in the right direction).
    if (res != null && np >= 0) {
      if (dir == Direction.FORWARDS) {
        if (np < res || res < start) {
          res = np
        }
      } else {
        if (np > res || res >= start && !countCurrent) {
          res = np
        }
      }
    } else if (res == null && np >= 0) {
      res = np
    }
    // else we found neither, res already -1
    return res
  }

  @Contract(pure = true)
  private fun findSentenceEnd(
    editor: VimEditor,
    chars: CharSequence,
    start: Int,
    max: Int,
    dir: Direction,
    countCurrent: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    if ((dir == Direction.BACKWARDS && start <= 0) || (dir == Direction.FORWARDS && start >= editor.fileSize()
        .toInt())
    ) {
      return null
    }

    // Save off the next paragraph since a paragraph is a valid sentence.
    val lline: Int = editor.offsetToBufferPosition(start).line
    var np: Int = findNextParagraph(editor, lline, dir, false)

    // Sections are also end-of-sentence markers. However, { and } in column 1 don't count.
    // Since our section implementation only supports these and form-feed chars, we'll just
    // check for form-feeds below.
    var res: Int? = null
    var offset = start
    var found = false
    // Search forward looking for a candidate end-of-sentence character (., !, or ?)
    while (offset >= 0 && offset < max && !found) {
      var ch = chars[offset]
      if (".!?".indexOf(ch) >= 0) {
        val end = offset // Save where we found the punctuation.
        offset++
        // This can be followed by any number of ), ], ", or ' characters.
        while (offset < max) {
          ch = chars[offset]
          if (")]\"'".indexOf(ch) == -1) {
            break
          }
          offset++
        }

        // The next character must be whitespace for this to be a valid end-of-sentence.
        if (offset >= max || Character.isWhitespace(ch)) {
          // So we have found the end of the next sentence. Now let's see if we ended
          // where we started (or further) on a back search. This will happen if we happen
          // to start this whole search already on a sentence end.
          if (offset - 1 == start && !countCurrent) {
            // Skip back to the sentence end so we can search backward from there
            // for the real previous sentence.
            offset = end
          } else {
            // Yeah - we found the real end-of-sentence. Save it off.
            res = offset - 1
            found = true
          }
        } else {
          // Turned out not to be an end-of-sentence so move back to where we were.
          offset = end
        }
      } else if (ch == '\n') {
        val end = offset // Save where we found the newline.
        if (dir == Direction.FORWARDS) {
          offset++
          while (offset < max) {
            ch = chars[offset]
            if (ch != '\n') {
              offset--
              break
            }
            if (offset == np && (end - 1 != start || countCurrent)) {
              break
            }
            offset++
          }
          if (offset == np && (end - 1 != start || countCurrent)) {
            res = end - 1
            found = true
          } else if (offset > end) {
            res = offset
            np = res
            found = true
          } else if (offset == end) {
            if (offset > 0 && chars[offset - 1] == '\n' && countCurrent) {
              res = end
              np = res
              found = true
            }
          }
        } else {
          if (offset > 0) {
            offset--
            while (offset > 0) {
              ch = chars[offset]
              if (ch != '\n') {
                offset++
                break
              }
              offset--
            }
          }
          if (offset < end) {
            res = if (end == start && countCurrent) {
              end
            } else {
              offset - 1
            }
            found = true
          }
        }
        offset = end
      } else if (ch == '\u000C') {
        res = offset
        found = true
      }
      offset += dir.toInt()
    }

    // Now let's see which to return, the sentence we found or the paragraph we found.
    // This mess returns which ever is closer to our starting point (and in the right direction).
    if (res != null && np >= 0) {
      if (dir == Direction.FORWARDS) {
        if (np < res || res < start) {
          res = np
        }
      } else {
        if (np > res || res >= start && !countCurrent) {
          res = np
        }
      }
    }
    return res
  }

  @Contract(pure = true)
  private fun findSentenceRangeEnd(
    editor: VimEditor,
    chars: CharSequence,
    start: Int,
    max: Int,
    count: Int,
    isOuter: Boolean,
    oneway: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    var count = count
    val dir = if (count > 0) Direction.FORWARDS else Direction.BACKWARDS
    count = Math.abs(count)
    count
    val toggle = !isOuter
    var findend = dir == Direction.BACKWARDS
    // Even = start, odd = end
    var which: Int
    val eprev = findSentenceEnd(editor, chars, start, max, Direction.BACKWARDS, true)
    val enext = findSentenceEnd(editor, chars, start, max, Direction.FORWARDS, true)
    val sprev = findSentenceStart(editor, chars, start, max, Direction.BACKWARDS, true)
    val snext = findSentenceStart(editor, chars, start, max, Direction.FORWARDS, true)
    if (snext == eprev) // On blank line
    {
      if (dir == Direction.BACKWARDS && !oneway) {
        return start
      }
      which = 0
      if (oneway) {
        findend = dir == Direction.FORWARDS
      } else if (dir == Direction.FORWARDS && start < max - 1 && !Character.isSpaceChar(chars[start + 1])) {
        findend = true
      }
    } else if (start == snext) // On sentence start
    {
      if (dir == Direction.BACKWARDS && !oneway) {
        return start
      }
      which = if (dir == Direction.FORWARDS) 1 else 0
      if (dir == Direction.BACKWARDS && oneway) {
        findend = false
      }
    } else if (start == enext) // On sentence end
    {
      if (dir == Direction.FORWARDS && !oneway) {
        return start
      }
      which = 0
      if (dir == Direction.FORWARDS && oneway) {
        findend = true
      }
    } else if ((sprev == null || start >= sprev) && (enext == null || start <= enext && snext != null && enext < snext)) // Middle of sentence
    {
      which = if (dir == Direction.FORWARDS) 1 else 0
    } else  // Between sentences
    {
      which = if (dir == Direction.FORWARDS) 0 else 1
      if (dir == Direction.FORWARDS) {
        if (oneway) {
          if (snext != null && start < snext - 1) {
            findend = true
          } else if (snext != null && start == snext - 1) {
            count++
          }
        } else {
          findend = true
        }
      } else {
        if (oneway) {
          if (eprev != null && start > eprev + 1) {
            findend = false
          } else if (eprev != null && start == eprev + 1) {
            count++
          }
        } else {
          findend = true
        }
      }
    }
    var res: Int? = start
    while (count > 0 && res != null && res >= 0 && res <= max - 1) {
      res = if (toggle && which % 2 == 1 || isOuter && findend) {
        findSentenceEnd(editor, chars, res, max, dir, false)
      } else {
        findSentenceStart(editor, chars, res, max, dir, false)
      }
      if (res == 0 || res == max - 1 || res == null) {
        count--
        break
      }
      if (toggle) {
        if (which % 2 == 1 && dir == Direction.BACKWARDS) {
          res++
        } else if (which % 2 == 0 && dir == Direction.FORWARDS) {
          res--
        }
      }
      which++
      count--
    }
    if (res == null || count > 0) {
      res = if (dir == Direction.FORWARDS) (if (max > 0) max - 1 else 0) else 0
    } else if (isOuter && (dir == Direction.BACKWARDS && findend || dir == Direction.FORWARDS && !findend)) {
      if (res != 0 && res != max - 1) {
        res -= dir.toInt()
      }
    }
    if (chars[res] == '\n' && res > 0 && chars[res - 1] != '\n') {
      res--
    }
    return res
  }

  @Contract(value = "_, _, _, _ -> new", pure = true)
  override fun findSentenceRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange {
    val chars: CharSequence = editor.text()
    if (chars.length == 0) return TextRange(0, 0)
    val max: Int = editor.fileSize().toInt()
    val offset: Int = caret.offset
    val ssel: Int = caret.selectionStart
    val esel: Int = caret.selectionEnd
    return if (Math.abs(esel - ssel) > 1) {
      val start: Int
      val end: Int
      // Forward selection
      if (offset == esel - 1) {
        start = ssel
        end = findSentenceRangeEnd(editor, chars, offset, max, count, isOuter, true)
        TextRange(start, end + 1)
      } else {
        end = esel - 1
        start = findSentenceRangeEnd(editor, chars, offset, max, -count, isOuter, true)
        TextRange(end, start + 1)
      }
    } else {
      val end = findSentenceRangeEnd(editor, chars, offset, max, count, isOuter, false)
      var space = isOuter
      if (Character.isSpaceChar(chars[end])) {
        space = false
      }
      val start = findSentenceRangeEnd(editor, chars, offset, max, -1, space, false)
      TextRange(start, end + 1)
    }
  }

  @Contract(pure = true)
  override fun findNextParagraph(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowBlanks: Boolean): @Range(
    from = 0,
    to = Int.MAX_VALUE.toLong()
  ) Int? {
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
  @Contract(pure = true)
  private fun findNextParagraph(editor: VimEditor, startLine: Int, direction: Direction, allowBlanks: Boolean): @Range(
    from = 0,
    to = Int.MAX_VALUE.toLong()
  ) Int {
    val line: Int? = findNextParagraphLine(editor, startLine, direction, allowBlanks)
    return if (line == null) {
      if (direction == Direction.FORWARDS) editor.fileSize().toInt() - 1 else 0
    } else {
      editor.getLineStartOffset(line)
    }
  }

  @Contract(pure = true)
  override fun findParagraphRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
    val line: Int = caret.getBufferPosition().line

    if (logger.isDebug()) {
      logger.debug("Starting paragraph range search on line $line")
    }

    val rangeInfo =
      (if (isOuter) findOuterParagraphRange(editor, line, count) else findInnerParagraphRange(editor, line, count))
        ?: return null
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

  @Contract(pure = true)
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
      startLine = if (editor.isLineEmpty(startLine, true)) findLastEmptyLine(
        editor,
        startLine,
        Direction.BACKWARDS
      ) else startLine
    }
    if (expandEnd) {
      endLine =
        if (editor.isLineEmpty(endLine, true)) findLastEmptyLine(editor, endLine, Direction.FORWARDS) else endLine
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
    startLine =
      if (editor.isLineEmpty(startLine, true)) findLastEmptyLine(editor, startLine, Direction.BACKWARDS) else startLine
    endLine = if (editor.isLineEmpty(endLine, true)) findLastEmptyLine(editor, endLine, Direction.FORWARDS) else endLine
    return Pair(startLine, endLine)
  }

  /**
   * If we have multiple consecutive empty lines in an editor, the method returns the first
   * or last empty line in the group of empty lines, depending on the specified direction
   */
  @Contract(pure = true)
  private fun findLastEmptyLine(editor: VimEditor, line: Int, direction: Direction): @Range(
    from = 0,
    to = Int.MAX_VALUE.toLong()
  ) Int {
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
  @Contract(pure = true)
  private fun findNextParagraphLine(
    editor: VimEditor,
    startLine: Int,
    count: Int,
    allowBlanks: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
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
  @Contract(pure = true)
  private fun findNextParagraphLine(
    editor: VimEditor,
    startLine: Int,
    direction: Direction,
    allowBlanks: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
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
  @Contract(pure = true)
  private fun skipEmptyLines(
    editor: VimEditor,
    startLine: Int,
    direction: Direction,
    allowBlanks: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    var i = startLine
    while (i in 0 until editor.nativeLineCount()) {
      if (!editor.isLineEmpty(i, allowBlanks)) break
      i += direction.toInt()
    }
    return i
  }

  override fun findWordUnderCursor(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    dir: Int,
    isOuter: Boolean,
    isBig: Boolean,
    hasSelection: Boolean,
  ): TextRange {
    logger.debug("count=$count")
    logger.debug("dir=$dir")
    logger.debug("isOuter=$isOuter")
    logger.debug("isBig=$isBig")
    logger.debug("hasSelection=$hasSelection")

    val chars: CharSequence = editor.text()
    val max: Int = editor.fileSize().toInt()
    if (max == 0) return TextRange(0, 0)

    logger.debug("max=$max")

    val pos: Int = caret.offset
    if (chars.length <= pos) return TextRange(chars.length - 1, chars.length - 1)

    val onSpace = charType(editor, chars[pos], isBig) === CharacterHelper.CharacterType.WHITESPACE

    // Find word start. Note that the caret might be on the word start, but the selection start might not be!
    val onWordStart = pos == 0 || charType(editor, chars[pos - 1], isBig) !== charType(editor, chars[pos], isBig)
    var start = pos

    logger.debug("pos=$pos")
    logger.debug("onWordStart=$onWordStart")

    // TODO: This could be simplified to move backwards until char type changes
    if ((!onWordStart && !(onSpace && isOuter)) || hasSelection || (count > 1 && dir == -1)) {
      start = if (dir == 1) {
        oldFindNextWordOne(editor.text(), editor, pos, editor.text().length, -1, isBig, spaceWords = !isOuter)
      } else {
        val c = -(count - if (onWordStart && !hasSelection) 1 else 0)
        oldFindNextWordOne(editor.text(), editor, pos, editor.text().length, c, isBig, spaceWords = !isOuter)
      }
      start = editor.normalizeOffset(start, false)
    }

    logger.debug("start=$start")

    // Find word end
    val onWordEnd = pos >= max - 1 || charType(editor, chars[pos + 1], isBig) !== charType(editor, chars[pos], isBig)

    logger.debug("onWordEnd=$onWordEnd")

    var end = pos

    // TODO: Figure out the logic of this going backwards
    if (dir == 1) {
      var count = count
      var shouldEndOnWhitespace = false

      // Selecting word/WORDs (forwards):
      // If there's no selection, we need to calculate the first range:
      // -> Move back to the first character _on the current line_ of the current character type
      //    If we're on whitespace, this is the start of the preceding whitespace
      //    If we're on a word/WORD char, it's the start of the word/WORD
      // -> Move forward to the end of the next word/WORD or whitespace block
      //    (Remember that `${se}` in the following examples is at `end+1`)
      //    For outer objects and currently on whitespace, move to end of the next word/WORD         ("${s}     word${se}")
      //      New lines are treated as whitespace, so this will wrap and move to the end of the next word/WORD.
      //      Empty lines will be treated as a word.
      //    For outer objects on a word/WORD char, move to one character _before_ the next word/WORD ("${s}word ${se}word")
      //      New lines should be treated as a stop character, and we stop before the new line.
      //    For inner objects on a word/WORD char, move to the end of this word/WORD                 ("${s}word${se} word")
      //      This will never encounter a new line character.
      //    For inner objects on whitespace, move to one character _before_ the next word/WORD       ("${s}     ${se}word")
      //      New lines should be treated as a stop character, and we stop before the new line.
      // -> Subtract 1 from count
      // Once we have a range, or if there's an initial selection:
      // -> Loop over count
      //   -> For inner objects, move to the end of the next character type block. Whitespace counts in the loop
      //   -> For outer objects, move to the character _before_ the next word/WORD. Therefore, whitespace does not count
      // For all of these operations, remember that an empty line is a word.

      if (!hasSelection) {
        // Move back to the first character of the current character type on the current line.
        // This will be the start of the word/WORD or the start of whitespace.
        val startingCharacterType = charType(editor, chars[pos], isBig)
        start = pos
        if (!isEmptyLine(chars, start)) {
          while (start >= 0 && chars[start] != '\n' && charType(editor, chars[start], isBig) == startingCharacterType) {
            start--
          }
          start++
        }

        // Move forward, including or skipping whitespace as necessary. Move from the start of the current
        // word/whitespace rather than the original position, so that it's easier to handle moving to the end of a word
        // when the original position is already at the end of the word.
        // Note that `onSpace` is the character type of the original position, but this is also the character type of
        // the current start position
        end = when {
          // We're on preceding whitespace. Include it, and move to the end of the next word/WORD. Newlines are
          // considered whitespace and this can wrap to the next line. An empty line will be considered a word and
          // included.
          isOuter && onSpace ->             // "${s}     word${se}"
            findNextWordEnd(editor, start, 1, isBig, stopOnEmptyLine = true, allowMoveFromWordEnd = false)

          // We're on a word, move to the end, and include following whitespace by moving to the character before the
          // next word. Newlines are not considered part of whitespace, not included, and this does not wrap.
          isOuter && !onSpace -> {          // "${s}word ${se}word"
            shouldEndOnWhitespace = true

            // Outer object should include following whitespace. Skip forward over the current word and following
            // whitespace. We know this isn't an empty line, and that we'll stop at the end of line, so it's always safe
            // to move back one character.
            val offset = findNextWordOne(chars, editor, start, isBig, stopAtEndOfLine = true)
            skipOneCharacterBack(offset)
          }

          // We're on a word, move to the end, not including trailing whitespace. This never includes whitespace, and so
          // never wraps
          !isOuter && !onSpace ->           // "${s}word${se} word"
            findNextWordEnd(editor, start, 1, isBig, stopOnEmptyLine = true, allowMoveFromWordEnd = false)

          // We're on preceding whitespace, move to the character before the next word. Newlines are not considered
          // whitespace and this does not wrap. Empty lines also do not wrap.
          else /* !isOuter && onSpace */ -> { // "${s}     ${se}word"

            // Inner object does not include whitespace, but does count it. Skip forward over the current whitespace
            // until we find a new word or the end of line. The implementation of `findNextWordOne` will always move at
            // least one character forward, so it's always safe to move one character back. If we are on an empty line,
            // `findNextWordOne` will still move one character forward, taking us to the next line. Moving one back will
            // return us to the original offset. You can see this with `viw` on an empty line - it only selects the
            // current line.
            val offset = findNextWordOne(chars, editor, start, isBig, stopAtEndOfLine = true)
            skipOneCharacterBack(offset)
          }
        }

        count--
      } else {
        end = pos
      }

      // We cannot rely on the current location of the cursor/"end". If there was no initial selection, then it will be
      // at the end of a character type block, either word/WORD or whitespace. But if there was an initial selection,
      // it could be anywhere.
      repeat (count) {
        if (isOuter) {
          // Outer object. Include whitespace.
          //
          // Selection ends on whitespace: Move to end of next word (skips whitespace, including newline)
          //   "${s}  ${se}      word   " -> "${s}        word${se}   "
          //   "${s}  ${se} \n   word   " -> "${s}  \n    word${se}   "
          // Selection ends on end of whitespace:  Move to one character before next word (or end of file)
          //   "${s}word    ${se}word   " -> "${s}word    word   ${se}"
          // Selection ends on word: Move to one character before next word
          //   "${s}wo${se}rd       word" -> "${s}word       ${se}word"
          //   "${s}wo${se}rd,      word" -> "${s}word${se},      word"
          // Selection ends on end of word: Move to end of next word (skips whitespace)
          //   "${s}word${se}    word   " -> "${s}word    word${se}   "
          // Selection ends on end of word with following word: Move to one character before next word
          //   "${s}word${se},   word   " -> "${s}word,   ${se}word   "
          // Selection ends on word char at end of line:
          //   If next non-newline char is word, move past newline, move to one char before next word
          //   Else, move to end of next word
          //   "${s}word${se}\nword     " -> "${s}word\nword     ${se}"
          //   "${s}  word${se}\nword   " -> "${s}  word\nword   ${se}"
          //   "${s}word${se}\n  word   " -> "${s}word\n  word${se}   "
          // Selection ends on whitespace at end of line:
          //   If next non-newline char is word, move past newline, move to one char before next word
          //   Else, move to end of next word
          //   "${s}    ${se}\nword     " -> "${s}    \nword     ${se}"
          //   "${s}    ${se}\n  word   " -> "${s}    \n  word${se}   "
          //
          // This can be generalised to move forward one char, skip again if it's a newline, then either move to the end
          // of the next word (which skips preceding whitespace), or to one character before the next word (which
          // includes following whitespace). Moving forward one char means we don't have to distinguish between inside a
          // word/whitespace, or at the end of a word whitespace. If we started inside, we want to move based on the
          // current/starting character type, if we started at the end, we want to move based on the next character
          // type. By always using next, we use the correct character type.

          // Move forward one char
          // Skip again if new char is newline
          // If on whitespace, move to end of next word (skips current/preceding whitespace)
          // If on word, move to one before start of next word (skips following whitespace)

          // Increment, and skip the newline char, unless we've just landed on an empty line
          end++
          if (end < chars.length && chars[end] == '\n' && !isEmptyLine(chars, end)) {
            end++
          }

          if (end >= chars.length) {
            end--
            return@repeat
          }

          end = if (isWhitespace(editor, chars[end], isBig)) {
            // Move to end of next word (skips current/preceding whitespace)
            findNextWordEnd(editor, end, 1, isBig, stopOnEmptyLine = true)
          }
          else {
            // Outer object includes whitespace. Starting on a word character, skip to the end of the current word and
            // then move one character back. Since we're on a word character, we know this isn't an empty line, and we
            // will therefore always move forward, and so it is always safe to move one character back.
            val offset = findNextWordOne(chars, editor, end, isBig, stopAtEndOfLine = true)
            skipOneCharacterBack(offset)
          }

        } else {
          // Inner object. Whitespace is not included in a move, but included as a separate (counted) move
          //
          // Selection ends on whitespace: Move to end of current character type or end of line.
          //   Or: move to one char before next word
          //   "${s}  ${se}         word" -> "${s}           ${se}word"
          //   "${s}  ${se} \n      word" -> "${s}   ${se}\n      word"
          // Selection ends on end of whitespace: Move to end of next character type.
          //   Or: move to end of next word
          //   "${s}           ${se}word" -> "${s}           word${se}"
          //   "${s}   ${se}\nword      " -> "${s}   \nword${se}      "
          //   "${s}   ${se}\n      word" -> "${s}   \n      ${se}word" // End of next word doesn't work here
          // Selection ends on word: Move to end of current character type.
          //   Or: move to end of current word
          //   "${s}wo${se}rd       word" -> "${s}word${se}       word"
          //   "${s}wo${se}rd,      word" -> "${s}word${se},      word"
          // Selection ends on end of word: Move to end of next character type or end of line.
          //   Or: move to one before next word, or end of line
          //   "${s}word${se}    word   " -> "${s}word    ${se}word   "
          //   "${s}word${se},   word   " -> "${s}word,${se}   word   " // One before next word doesn't work here
          //   "${s}word${se}  \n  word " -> "${s}word  ${se}\n  word "
          // Selection ends on word char at end of line: Move to end of next character type SKIPPING NEWLINE!
          //   Or: move to end of next word
          //   "${s}word${se}\nword     " -> "${s}word\nword${se}     "
          //   "${s}  word${se}\nword   " -> "${s}  word\nword${se}   "
          //   "${s}word${se}\n  word   " -> "${s}word\n  ${se}word   " // End of next word doesn't work here
          // Selection ends on whitespace at end of line: Move to end of next character type SKIPPING NEWLINE!
          //   Or: move to one before next word
          //   "${s}word    ${se}\n    word" -> "${s}word    \n    ${se}word"
          //   "${s}word    ${se}\nword    " -> "${s}word    \nword${se}    " // Doesn't work
          //
          // This can be generalised to move forward on character, skip again if it's a newline, then move to end of
          // the now current character type.

          // Increment, and skip the newline char, unless we've just landed on an empty line
          end++
          if (end < chars.length && chars[end] == '\n' && !isEmptyLine(chars, end)) {
            end++
          }

          if (end >= chars.length) {
            end--
            return@repeat
          }

          end = if (isWhitespace(editor, chars[end], isBig)) {
            // Inner object does not include whitespace, but does count it. Skip to the end of the whitespace by moving
            // to one character before the next word or end of the current line.
            // For a non-empty line, it is always possible to move forward and so it is always safe to move one
            // character back.
            // Things get weird with empty lines. When handling empty lines above (when there is no initial selection),
            // we try to get to the character before the next word. We advance, wrap to the next line, and stop because
            // we're on an empty line (normal before for e.g. `w`). We then come back one character and that puts us
            // back at the initial offset, and the caret doesn't move.
            // Vim does things differently if there's an existing selection, and we're moving on to an empty line. The
            // algorithm needs to see what the next character is, so we move one char forward. This skips us past a
            // newline char and onto an empty line. We then try to find the next word which automatically advances one,
            // onto the start of the next line. And now Vim does NOT go back one character, because that would put us
            // at the newline of the previous line.
            // Interestingly, because we're not at the start of another line, this one might not be empty. But Vim still
            // does not move back one, leading to an odd scenario where `iw` can select the *first* character of a word
            // after whitespace/empty lines. See vim/vim#16514
            // By refusing to move back even if the current line isn't empty, we're matching Vim's quirky behaviour!
            val offset = findNextWordOne(chars, editor, end, isBig, stopAtEndOfLine = true)
            skipOneCharacterBackOnCurrentLine(chars, offset)
          }
          else {
            // Skip to the end of the current word. This would skip preceding whitespace, but we know we're on a word
            // character.
            findNextWordEnd(editor, end, 1, isBig, stopOnEmptyLine = true, allowMoveFromWordEnd = false)
          }
        }
      }

      if (isOuter && shouldEndOnWhitespace && start > 0
        && !isWhitespace(editor, chars[end], isBig)
        && !isWhitespace(editor, chars[start], isBig)) {

        // Outer word objects normally include following whitespace. But if there's no following whitespace to include,
        // we should extend the range to include preceding whitespace. However, Vim doesn't select whitespace at the
        // start of a line
        var offset = start - 1
        while (offset >= 0 && chars[offset] != '\n' && isWhitespace(editor, chars[offset], isBig)) {
          offset--
        }
        if (offset > 0 && chars[offset] != '\n') start = offset + 1
      }

      if (start == end && chars[start] == '\n') end++
      return TextRange(start, end + 1)
    }
    else if (!onWordEnd || hasSelection || (count > 1 && dir == 1) || (onSpace && isOuter)) {
      end = if (dir == 1) {
        val c = count - if (onWordEnd && !hasSelection && (!(onSpace && isOuter) || (onSpace && !isOuter))) 1 else 0
        findNextWordEnd(editor, pos, c, isBig, !isOuter, allowMoveFromWordEnd = false)
      } else {
        findNextWordEnd(editor, pos, 1, isBig, !isOuter, allowMoveFromWordEnd = false)
      }
    }

    logger.debug("end=$end")

    val hasForwardHeadingSelection = dir == 1 && hasSelection
    val hasBackwardHeadingSelection = dir == -1 && hasSelection
    val hasFollowingWhitespace = if (end < max - 1) {
      val c = chars[end + 1]
      charType(editor, c, false) === CharacterHelper.CharacterType.WHITESPACE && c != '\n'
    }
    else {
      false
    }

    val includePrecedingWhitespace = if (isOuter) {
      // Outer word motion. Include preceding whitespace:
      // ✗ NEVER: Has forward-facing selection
      // ✓ Started on space, and there's no (forward-facing) selection
      // ✓ Started on word and has backward-facing selection
      // ✓ No whitespace after word under cursor (see `:help v_a'`), but only if there's a preceding word on the line
      !hasForwardHeadingSelection
        && ((onSpace && !hasSelection)
          || (hasBackwardHeadingSelection && !onSpace)
          || (!hasFollowingWhitespace && editor.anyNonWhitespace(start, -1)))
    }
    else {
      // Inner word motion. Include preceding whitespace:
      // ✓ Start on space with backwards-facing selection
      // ✓ Start on space with no (forwards-facing) selection
      onSpace && (hasBackwardHeadingSelection || !hasForwardHeadingSelection)
    }

    // Include following whitespace:
    // * ALWAYS: outer word motions with forward direction, has following whitespace to select, and we're not already
    //   about to extend the range with preceding whitespace (Vim usually only expands in one direction)
    // * AND:
    // ✓ Does not have a selection
    // ✓ Has a selection that does not start on (preceding) whitespace
    // ✓ The range between caret offset (exclusive) and end of word does not contain whitespace
    //   This last one is subtle, and means we can expand in both directions (perhaps only through repeated motions,
    //   such as `vawaw`). Examples:
    //   * Wrapping across newlines. On the last word, there is no following whitespace, so we select preceding
    //     whitespace. Repeating the motion expands to the end of the next word on a subsequent line. But if that word
    //     has preceding whitespace, even on a prior line, then we don't expand the range to following whitespace
    //   * If the next word is not space, but a non-word character, then we expand to include following whitespace
    val selectionStartOnSpace = hasSelection && charType(editor, chars[caret.vimSelectionStart], isBig) === CharacterHelper.CharacterType.WHITESPACE
    val hasIntermediateWhitespace =
      (pos + 1 < max && chars[pos + 1] != '\n' && charType(editor, chars[pos + 1], isBig) === CharacterHelper.CharacterType.WHITESPACE)
      || (pos + 2 < max && chars[pos + 1] == '\n' && charType(editor, chars[pos + 2], isBig) === CharacterHelper.CharacterType.WHITESPACE)
    val includeFollowingWhitespace = isOuter && dir == 1
      && !includePrecedingWhitespace && hasFollowingWhitespace
      && (!hasSelection || (!selectionStartOnSpace && !hasIntermediateWhitespace) || !hasIntermediateWhitespace)

    logger.debug("goBack=$includePrecedingWhitespace")
    logger.debug("goForward=$includeFollowingWhitespace")

    if (includeFollowingWhitespace) {
      while (end + 1 < max
        && chars[end + 1] != '\n'
        && charType(editor, chars[end + 1], false) === CharacterHelper.CharacterType.WHITESPACE
      ) {
        end++
      }
    }
    if (includePrecedingWhitespace) {
      while (start > 0
        && chars[start - 1] != '\n'
        && charType(editor, chars[start - 1], false) === CharacterHelper.CharacterType.WHITESPACE
      ) {
        start--
      }
    }

    logger.debug("start=$start")
    logger.debug("end=$end")

    // TODO: Remove this when IdeaVim supports selecting the new line character
    // A selection with start == end is perfectly valid, and will select a single character. However, IdeaVim
    // unnecessarily prevents selecting the new line character at the end of a line. If the selection is just that new
    // line character, then nothing is selected (we end up with a selection with range start==endInclusive, rather than
    // start==endExclusive). This little hack makes sure that `viw` will (mostly) work on a single empty line
    if (start == end && chars[start] == '\n') end++

    // Text range's end offset is exclusive
    return TextRange(start, end + 1)
  }

  override fun findBlockTagRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
    var counter = count
    var isOuterVariable = isOuter
    val position: Int = caret.offset
    val sequence: CharSequence = editor.text()

    val selectionStart: Int = caret.selectionStart
    val selectionEnd: Int = caret.selectionEnd

    val isRangeSelection = selectionEnd - selectionStart > 1

    var searchStartPosition: Int
    searchStartPosition = if (!isRangeSelection) {
      val line: Int = caret.getBufferPosition().line
      val lineBegin: Int = editor.getLineStartOffset(line)
      ignoreWhitespaceAtLineStart(sequence, lineBegin, position)
    } else {
      selectionEnd
    }

    if (isInHTMLTag(sequence, searchStartPosition, false)) {
      // caret is inside opening tag. Move to closing '>'.
      while (searchStartPosition < sequence.length && sequence[searchStartPosition] != '>') {
        searchStartPosition++
      }
    } else if (isInHTMLTag(sequence, searchStartPosition, true)) {
      // caret is inside closing tag. Move to starting '<'.
      while (searchStartPosition > 0 && sequence[searchStartPosition] != '<') {
        searchStartPosition--
      }
    }

    while (true) {
      val (closingTagTextRange, tagName) = findUnmatchedClosingTag(sequence, searchStartPosition, counter)
        ?: return null
      val openingTag = findUnmatchedOpeningTag(sequence, closingTagTextRange.startOffset, tagName)
        ?: return null
      if (isRangeSelection && openingTag.endOffset - 1 >= selectionStart) {
        // If there was already some text selected and the new selection would not extend further, we try again
        searchStartPosition = closingTagTextRange.endOffset
        counter = 1
        continue
      }
      var selectionEndWithoutNewline = selectionEnd
      while (selectionEndWithoutNewline < sequence.length && sequence[selectionEndWithoutNewline] == '\n') {
        selectionEndWithoutNewline++
      }
      val mode = editor.mode
      if (mode is Mode.VISUAL) {
        if (closingTagTextRange.startOffset == selectionEndWithoutNewline &&
          openingTag.endOffset == selectionStart
        ) {
          // Special case: if the inner tag is already selected we should like isOuter is active
          // Note that we need to ignore newlines, because their selection is lost between multiple "it" invocations
          isOuterVariable = true
        } else if (openingTag.endOffset == closingTagTextRange.startOffset &&
          selectionStart == openingTag.endOffset
        ) {
          // Special case: for an empty tag pair (e.g. <a></a>) the whole tag is selected if the caret is in the middle.
          isOuterVariable = true
        }
      }
      return if (isOuterVariable) {
        TextRange(openingTag.startOffset, closingTagTextRange.endOffset)
      } else {
        TextRange(openingTag.endOffset, closingTagTextRange.startOffset)
      }
    }
  }

  /**
   * returns new position which ignore whitespaces at beginning of the line
   */
  private fun ignoreWhitespaceAtLineStart(seq: CharSequence, lineStart: Int, pos: Int): Int {
    var position = pos
    if (seq.subSequence(lineStart, position).chars().allMatch { codePoint: Int ->
        Character.isWhitespace(
          codePoint
        )
      }) {
      while (position < seq.length && seq[position] != '\n' && Character.isWhitespace(seq[position])) {
        position++
      }
    }
    return position
  }

  /**
   * Returns true if there is a html at the given position. Ignores tags with a trailing slash like <aaa></aaa>.
   */
  private fun isInHTMLTag(sequence: CharSequence, position: Int, isEndtag: Boolean): Boolean {
    var openingBracket = -1
    run {
      var i = position
      while (i >= 0 && i < sequence.length) {
        if (sequence[i] == '<') {
          openingBracket = i
          break
        }
        if (sequence[i] == '>' && i != position) {
          return false
        }
        i--
      }
    }
    if (openingBracket == -1) {
      return false
    }
    val hasSlashAfterOpening = openingBracket + 1 < sequence.length && sequence[openingBracket + 1] == '/'
    if (isEndtag && !hasSlashAfterOpening || !isEndtag && hasSlashAfterOpening) {
      return false
    }
    var closingBracket = -1
    for (i in openingBracket until sequence.length) {
      if (sequence[i] == '>') {
        closingBracket = i
        break
      }
    }
    return closingBracket != -1 && sequence[closingBracket - 1] != '/'
  }

  private fun findUnmatchedOpeningTag(
    sequence: CharSequence,
    position: Int,
    tagName: String,
  ): TextRange? {
    val quotedTagName = Pattern.quote(tagName)
    val patternString = ("(</%s>)" // match closing tags
      +
      "|(<%s" // or opening tags starting with tagName
      +
      "(\\s([^>]*" // After at least one whitespace there might be additional text in the tag. E.g. <html lang="en">
      +
      "[^/])?)?>)") // Slash is not allowed as last character (this would be a self closing tag).
    val tagPattern =
      Pattern.compile(String.format(patternString, quotedTagName, quotedTagName), Pattern.CASE_INSENSITIVE)
    val matcher = tagPattern.matcher(sequence.subSequence(0, position + 1))
    val openTags: Deque<TextRange> = ArrayDeque()
    while (matcher.find()) {
      val match = TextRange(matcher.start(), matcher.end())
      if (sequence[matcher.start() + 1] == '/') {
        if (!openTags.isEmpty()) {
          openTags.pop()
        }
      } else {
        openTags.push(match)
      }
    }
    return if (openTags.isEmpty()) {
      null
    } else {
      openTags.pop()
    }
  }

  private fun findUnmatchedClosingTag(
    sequence: CharSequence,
    position: Int,
    count: Int,
  ): Pair<TextRange, String>? {
    // The tag name may contain any characters except slashes, whitespace and '>'
    var counter = count
    val tagNamePattern = "([^/\\s>]+)"
    // An opening tag consists of '<' followed by a tag name, optionally some additional text after whitespace and a '>'
    val openingTagPattern = String.format("<%s(?:\\s[^>]*)?>", tagNamePattern)
    val closingTagPattern = String.format("</%s>", tagNamePattern)
    val tagPattern = Pattern.compile(String.format("(?:%s)|(?:%s)", openingTagPattern, closingTagPattern))
    val matcher = tagPattern.matcher(sequence.subSequence(position, sequence.length))
    val openTags: Deque<String> = ArrayDeque()
    while (matcher.find()) {
      val isClosingTag = matcher.group(1) == null
      if (isClosingTag) {
        val tagName = matcher.group(2)
        // Ignore unmatched open tags. Either the file is malformed or it might be a tag like <br> that does not need to be closed.
        while (!openTags.isEmpty() && !openTags.peek().equals(tagName, ignoreCase = true)) {
          openTags.pop()
        }
        if (openTags.isEmpty()) {
          if (counter <= 1) {
            return Pair(TextRange(position + matcher.start(), position + matcher.end()), tagName)
          } else {
            counter--
          }
        } else {
          openTags.pop()
        }
      } else {
        val tagName = matcher.group(1)
        openTags.push(tagName)
      }
    }
    return null
  }
}
