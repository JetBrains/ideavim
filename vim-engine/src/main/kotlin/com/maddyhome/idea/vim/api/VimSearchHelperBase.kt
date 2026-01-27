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
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inVisualMode
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
    line: Int,
    type: Char,
    direction: Int,
    count: Int,
  ): Int {
    val documentText: CharSequence = editor.text()
    var currentLine: Int = line + direction
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

  override fun findSection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    direction: Int,
    count: Int,
  ): Int {
    val line = caret.getBufferPosition().line
    return findSection(editor, line, type, direction, count)
  }

  override fun findNextCharacterOnLine(editor: VimEditor, offset: Int, count: Int, ch: Char): Int {
    val line = editor.offsetToBufferPosition(offset).line
    val start = editor.getLineStartOffset(line)
    val end = editor.getLineEndOffset(line, true)
    val chars: CharSequence = editor.text()
    var found = 0
    val step = if (count >= 0) 1 else -1
    var pos: Int = offset + step
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

  override fun findNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int {
    val offset = caret.offset
    return findNextCharacterOnLine(editor, offset, count, ch)
  }

  override fun findWordAtOrFollowingCursor(editor: VimEditor, offset: Int, isBigWord: Boolean): TextRange? {
    val chars = editor.text()
    val line = editor.offsetToBufferPosition(offset).line
    val stop = editor.getLineEndOffset(line, true)

    val pos = offset

    if (chars.isEmpty() || chars.length <= pos) return null

    var start = pos
    val types = arrayOf(
      CharacterHelper.CharacterType.KEYWORD,
      CharacterHelper.CharacterType.PUNCTUATION
    )
    for (i in 0..1) {
      start = pos
      val type = charType(editor, chars[start], isBigWord)
      if (type == types[i]) {
        // Search back for start of word
        while (start > 0 && charType(editor, chars[start - 1], isBigWord) == types[i]) {
          start--
        }
      } else {
        // Search forward for start of word
        while (start < stop && charType(editor, chars[start], isBigWord) != types[i]) {
          start++
        }
      }

      if (start != stop) {
        break
      }
    }

    if (start == stop) {
      return null
    }

    // Special case 1 character words because 'findNextWordEnd' returns one to many chars
    val end = if (start < stop &&
      (start >= chars.length - 1 ||
        charType(editor, chars[start + 1], isBigWord) != CharacterHelper.CharacterType.KEYWORD)
    ) {
      start + 1
    } else {
      injector.searchHelper.findNextWordEnd(editor, start, 1, isBigWord, false) + 1
    }

    return TextRange(start, end)
  }

  override fun findWordAtOrFollowingCursor(editor: VimEditor, caret: ImmutableVimCaret, isBigWord: Boolean): TextRange? {
    val offset = caret.offset
    return findWordAtOrFollowingCursor(editor, offset, isBigWord)
  }

  override fun findFilenameAtOrFollowingCursor(editor: VimEditor, caret: ImmutableVimCaret): TextRange? {
    return findFilenameAtOrFollowingCursor(editor, caret.offset)
  }
  override fun findFilenameAtOrFollowingCursor(editor: VimEditor, offset: Int): TextRange? {
    val text = editor.text()
    if (text.isEmpty()) return null

    val start = if (!KeywordOptionHelper.isFilename(editor, text[offset])) {
      moveForwardsToStartOfFilename(editor, text, offset)
    } else {
      moveBackwardsToStartOfFilename(editor, text, offset)
    }
    if (start == -1) return null

    val end = moveForwardsToEndOfFilename(editor, text, start)
    if (end == -1) return null

    return TextRange(start, end)
  }

  private fun moveForwardsToStartOfFilename(editor: VimEditor, text: CharSequence, start: Int): Int {
    var offset = start
    while (offset < text.length && !KeywordOptionHelper.isFilename(editor, text[offset])) {
      if (text[offset] == '\n') return -1
      offset++
    }
    if (offset == text.length) return -1
    return offset
  }

  private fun moveBackwardsToStartOfFilename(editor: VimEditor, text: CharSequence, start: Int): Int {
    var offset = start
    while (offset > 0 && KeywordOptionHelper.isFilename(editor, text[offset - 1])) {
      if (text[offset - 1] == '\n') return -1
      offset--
    }
    return offset
  }

  private fun moveForwardsToEndOfFilename(editor: VimEditor, text: CharSequence, start: Int): Int {
    var offset = start
    while (offset < text.length && text[offset] != '\n' && KeywordOptionHelper.isFilename(editor, text[offset])) {
      offset++
    }
    return offset - 1
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
  ): Int {
    val text = editor.text()
    var pos = searchFrom
    repeat(abs(count)) {
      pos = if (count > 0) {
        findNextWordEndOne(text, editor, pos, bigWord, stopOnEmptyLine, allowMoveFromWordEnd = true)
      } else {
        findPreviousWordEndOne(text, editor, pos, bigWord).coerceAtLeast(0)
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
      injector.messages.showErrorMessage(editor, e.message)
      return null
    }

    var result = if (dir === Direction.FORWARDS) {
      findNextWithWrapscan(editor, regex, startOffset, options, wrap, showMessages)
    } else {
      findPreviousWithWrapscan(editor, regex, startOffset, options, wrap, showMessages)
    }

    if (result is VimMatchResult.Failure) {
      if (wrap) {
        injector.messages.showErrorMessage(editor, injector.messages.message("E486", pattern))
      } else if (dir === Direction.FORWARDS) {
        injector.messages.showErrorMessage(editor, injector.messages.message("E385", pattern))
      } else {
        injector.messages.showErrorMessage(editor, injector.messages.message("E384", pattern))
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
            injector.messages.showErrorMessage(editor, injector.messages.message("E385", pattern))
          } else {
            injector.messages.showErrorMessage(editor, injector.messages.message("E384", pattern))
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
        injector.messages.showMessage(editor, injector.messages.message("message.search.hit.bottom"))
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
        injector.messages.showMessage(editor, injector.messages.message("message.search.hit.top"))
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
      injector.messages.showErrorMessage(editor, e.message)
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
    allowMoveFromWordStart: Boolean = true,
  ): Int {
    if (chars.isEmpty()) return 0

    var pos = start
    val startingCharType = charType(editor, chars[pos.coerceAtMost(chars.length - 1)], bigWord)

    // Always move back one to make sure that we don't get stuck on the start of a word
    pos--

    if (allowMoveFromWordStart
      || startingCharType == charType(editor, chars[pos], bigWord)
      || isWhitespace(editor, chars[pos], bigWord)
    ) {
      // Skip any intermediate whitespace, stopping at an empty line (offset is the newline char of the empty line).
      // This will leave us on the last character of the previous word.
      while (pos >= 0 && isWhitespace(editor, chars[pos], bigWord)) {
        if (isEmptyLine(chars, pos)) return pos
        pos--
      }

      if (pos <= 0) return 0

      // We're now on a word character, or at the start of the file. Move back until we're past the start of the word,
      // then move forward to the start of the word
      pos = skipWhileCharacterType(editor, chars, pos, -1, charType(editor, chars[pos], bigWord), bigWord)
    }

    return (pos + 1).coerceAtLeast(0)
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

  /**
   * Find the end of the previous word, skipping the current word and any intermediate whitespace
   *
   * Note that this will return `-1` if there is no previous word! This is necessary to distinguish between no previous
   * word and the previous word being on the last character of the file.
   */
  private fun findPreviousWordEndOne(
    chars: CharSequence,
    editor: VimEditor,
    start: Int,
    bigWord: Boolean,
    stopAtEndOfPreviousLine: Boolean = false,
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
      // Unlike moving forwards, we always check for empty lines.
      // If requested, we stop when we wrap at the start of the current line. Ideally, we would stop when we hit the
      // start of the current line (e.g. return `pos+1`), but this doesn't work with how the function is called. It's
      // used when finding the word under the cursor - we move to the character after the end of the previous word. It's
      // easier to return the newline at the end of the previous line so that adding one will move us to the start of
      // the current line.
      if (isEmptyLine(chars, pos) || (chars[pos] == '\n' && stopAtEndOfPreviousLine)) return pos
      pos--
    }

    return pos
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
    offset: Int,
    quote: Char,
    isOuter: Boolean,
  ): TextRange? {
    var leftQuote: Int
    var rightQuote: Int

    val caretOffset = offset
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
    } else {
      // For outer quotes: include trailing whitespace, or leading whitespace if no trailing
      // See :help a" - "Any trailing white space is included, unless there is none, then leading white space is included"
      val text = editor.text()
      var trailingEnd = rightQuote + 1

      // Check for trailing whitespace (on same line)
      while (trailingEnd < text.length && text[trailingEnd] == ' ') {
        trailingEnd++
      }

      if (trailingEnd > rightQuote + 1) {
        // Found trailing whitespace, include it
        rightQuote = trailingEnd - 1
      } else {
        // No trailing whitespace, check for leading whitespace
        var leadingStart = leftQuote
        while (leadingStart > 0 && text[leadingStart - 1] == ' ') {
          leadingStart--
        }
        if (leadingStart < leftQuote) {
          leftQuote = leadingStart
        }
      }
    }
    return TextRange(leftQuote, rightQuote + 1)
  }

  override fun findBlockQuoteInLineRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    quote: Char,
    isOuter: Boolean,
  ): TextRange? {
    val offset = caret.offset
    return findBlockQuoteInLineRange(editor, offset, quote, isOuter)
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
    offset: Int,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    var count = count
    val dir = if (count > 0) Direction.FORWARDS else Direction.BACKWARDS
    count = Math.abs(count)
    val total = count
    val chars: CharSequence = editor.text()
    val start: Int = offset
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
  override fun findNextSentenceStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    val offset = caret.offset
    return findNextSentenceStart(editor, offset, count, countCurrent, requireAll)
  }

  @Contract(pure = true)
  override fun findNextSentenceEnd(
    editor: VimEditor,
    offset: Int,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    var count = count
    val dir = if (count > 0) Direction.FORWARDS else Direction.BACKWARDS
    count = Math.abs(count)
    val total = count
    val chars: CharSequence = editor.text()
    val start: Int = offset
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
  override fun findNextSentenceEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int? {
    val offset = caret.offset
    return findNextSentenceEnd(editor, offset, count, countCurrent, requireAll)
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
  override fun findNextParagraph(editor: VimEditor, startLine: Int, count: Int, allowBlanks: Boolean): @Range(
    from = 0,
    to = Int.MAX_VALUE.toLong()
  ) Int? {
    val line: Int = findNextParagraphLine(editor, startLine, count, allowBlanks) ?: return null
    val lineCount: Int = editor.nativeLineCount()
    return if (line == lineCount - 1) {
      if (count > 0) editor.fileSize().toInt() - 1 else 0
    } else {
      editor.getLineStartOffset(line)
    }
  }

  @Contract(pure = true)
  override fun findNextParagraph(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowBlanks: Boolean): @Range(
    from = 0,
    to = Int.MAX_VALUE.toLong()
  ) Int? {
    val startLine = caret.getBufferPosition().line
    return findNextParagraph(editor, startLine, count, allowBlanks)
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
    line: Int,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
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
  override fun findParagraphRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
    val line = caret.getBufferPosition().line
    return findParagraphRange(editor, line, count, isOuter)
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

  override fun findWordObject(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
    isBig: Boolean,
  ): TextRange {
    // Note: for more detailed comments with examples, check git history!

    // Ignore single width selection. We still move both back and forwards with a single width selection
    val hasSelection = editor.inVisualMode && caret.vimSelectionStart != caret.offset
    val direction =
      if (hasSelection && caret.vimSelectionStart > caret.offset) Direction.BACKWARDS else Direction.FORWARDS

    val pos = caret.offset
    val chars = editor.text()
    if (chars.isEmpty()) return TextRange(0, 0)
    if (chars.length <= pos) return TextRange(chars.length - 1, chars.length - 1)

    var start = pos
    var end = start
    var count = count
    var shouldEndOnWhitespace = false

    // If there's no selection, calculate the initial range by moving back to the start of the current character type
    // on the current line (word/WORD or whitespace). Then move forward:
    // * For inner objects, move to the end of the current word or whitespace block (or line).
    // * For outer objects, whitespace is included. Move to the end of the current word (or line) and following
    //   whitespace (if any), or move to the end of the current whitespace (possibly wrapping) and following word.
    // Note that the flag for selection is only true if the selection is greater than a single char. Also remember
    // that an empty line is a word and there are multiple word types not necessarily separated by whitespace.
    if (!hasSelection) {
      val startingCharacterType = charType(editor, chars[pos], isBig)
      start = pos
      if (!isEmptyLine(chars, start)) {
        while (start >= 0 && chars[start] != '\n' && charType(editor, chars[start], isBig) == startingCharacterType) {
          start--
        }
        start++
      }

      end = if ((!isOuter && isWhitespace(editor, chars[start], isBig))
        || (isOuter && !isWhitespace(editor, chars[start], isBig))
      ) {
        // * Inner object, on whitespace. Skip forward to the end of the current whitespace, just before the next
        //   word or end of line (no wrapping). This will always move us forward one character, so it's always safe to
        //   move one character back. If we're moving on to an empty line (newline is whitespace!) this will move one
        //   character forward and then one character back. I.e. `viw` on an empty line only selects the line!
        // * Outer object, on word. Skip the current word and include any following whitespace. We know this isn't an
        //   empty line and that we'll stop at the end of the current line, so it's always safe to move back on char.
        if (isOuter) {
          // Outer objects should include following whitespace. But if there isn't any, we should walk back and
          // include any preceding whitespace.
          shouldEndOnWhitespace = true
        }

        val offset = findNextWordOne(chars, editor, start, isBig, stopAtEndOfLine = true)
        skipOneCharacterBack(offset)
      } else {
        // * Inner object, on word. Move to the end of the current word, do not bother with whitespace.
        // * Outer object, on whitespace. Include whitespace and the following word by moving to the end of the next
        //   word/WORD. Newlines are considered whitespace and so can wrap. Make sure that if we are currently at the
        //   end of a word (because we advanced above) that we do not advance to the end of the subsequent word.
        findNextWordEndOne(chars, editor, start, isBig, stopOnEmptyLine = true, allowMoveFromWordEnd = false)
      }

      count--
    }

    // We have an initial selection/range. Now loop over what's left of count.
    // The actions are very similar, but there is subtly different handling of newlines and empty lines.
    // Note that we can't make any assumptions about the start/end positions. If there was no initial selection, we know
    // that start is at the beginning of the initial character type and end will be either at the end of a word, end of
    // whitespace or end of line. But if there was an initial selection, it all depends on what the user selected.
    repeat(count) {
      when (direction) {
        Direction.FORWARDS -> {
          // Move forward (and skip end of line char) so we know if we need to move to the current or next word.
          // If we're at the end of a word, the next character will be a different character type/whitespace.
          // If we're in the middle of a word, the next character will still be the current word.
          end++
          if (end < chars.length && chars[end] == '\n' && !isEmptyLine(chars, end)) {
            end++
          }

          if (end >= chars.length) {
            end--
            return@repeat
          }

          end = if ((!isOuter && isWhitespace(editor, chars[end], isBig))
            || (isOuter && !isWhitespace(editor, chars[end], isBig))
          ) {
            // * Inner object, on whitespace. Whitespace is treated separately and included in the count. Skip the
            //   current whitespace, up to the character before the next word, or the end of the line.
            //   For a non-empty line, this will always move forwards, so it is always safe to move one char back.
            //   For empty lines, things are more complex and different to the behaviour above when we set the initial
            //   range. In that case, we advance while trying to get to the next word, encounter an empty line, stop,
            //   and then we always move one character back. This can put us back to the original offset (try `viw` on
            //   an empty line - the caret doesn't move).
            //   Vim does things differently if there's an existing selection (i.e., this scenario) and we're moving on
            //   to an empty line. The algorithm advances early (see above), and this skips us past a newline char and
            //   on to an empty line. We then try to find the next word, which automatically advances a character, on to
            //   the start of the next line. And now Vim does NOT go back one character, because that would put us at
            //   the newline char of the previous line.
            //   You can see this behaviour with `v2iw` on empty lines. Vim selects the first line while initialising
            //   the range, and then advances 2 lines while handling the second iteration. Similarly, `v3iw` selects 5
            //   lines. Interestingly, because we're now at the start of another line, the now-current line might not be
            //   empty. That means Vim now has a "word" text object that selects just the first character in a line!
            //   And because we've figured out this difference in handling empty lines, we match Vim's quirky behaviour!
            //   See vim/vim#16514
            // * Outer object, on a word character. Move to the end of the current word including following whitespace.
            //   This is the same as moving to the character before the next word. Also stop at the end of the current
            //   line. We know this isn't an empty line, so we will never wrap and will always move forward at least one
            //   character. It is therefore always safe to move back one character, without reaching the start of line.
            val offset = findNextWordOne(chars, editor, end, isBig, stopAtEndOfLine = true)
            skipOneCharacterBackOnCurrentLine(chars, offset)
          } else {
            // * Inner object, on a word character. Move to the end of the current word. This does not look at
            //   whitespace, and remains on the current line.
            // * Outer object, on whitespace. Move to the end of the next word, which will skip the current whitespace.
            //   Newline characters are whitespace, so this can wrap, although it will stop at an empty line. Make sure
            //   that if we are currently at the end of a word (because we advanced above) that we do not advance to the
            //   end of the subsequent word.
            findNextWordEndOne(chars, editor, end, isBig, stopOnEmptyLine = true, allowMoveFromWordEnd = false)
          }
        }

        Direction.BACKWARDS -> {
          // If direction is backwards, then `end` is already correctly positioned, and we need to move `start`.
          // As above, move back early so we handle word boundaries correctly
          start--
          if (start > 0 && chars[start] == '\n' && !isEmptyLine(chars, start)) {
            start--
          }

          if (start < 0) {
            start++
            return@repeat
          }

          start = if ((!isOuter && isWhitespace(editor, chars[start], isBig))
            || (isOuter && !isWhitespace(editor, chars[start], isBig))
          ) {
            // * Inner object, on whitespace. Move to start of whitespace, by moving to the end of the previous word and
            //   then moving forward. Newlines are whitespace, but we stop at the start of the line.
            // * Outer object, on word. Move to start of current word, then include and preceding whitespace, but stop
            //   at the start of line. This is the same as one past the end of the previous word.
            // Note that we actually stop at the end of the previous line, but the `+1` fixes things up.
            val offset = findPreviousWordEndOne(chars, editor, start, isBig, stopAtEndOfPreviousLine = true) + 1
            if (chars[offset] == '\n') offset + 1 else offset
          } else {
            // * Inner object, on word. Move back to the start of the current word. Ignore whitespace.
            // * Outer object, on whitespace. Skip the current whitespace and move to the start of the previous word.
            //   Newlines are whitespace, so this will wrap at the start of the line and move to the start of the last
            //   word on the previous line, skipping trailing whitespace.
            findPreviousWordOne(chars, editor, start, isBig, allowMoveFromWordStart = false)
          }
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
