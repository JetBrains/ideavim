/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import kotlin.math.max

const val BLOCK_CHARS: String = "{}()[]<>"

fun isInsideComment(editor: VimEditor, pos: Int): Boolean {
  return injector.psiService.getCommentAtPos(editor, pos) != null
}

/**
 * Determines whether the specified caret position is within the bounds of a single-quoted string in the editor
 * (uses PSI when possible)
 *
 * @param isInner A flag indicating the behavior regarding quote characters themselves:
 *                - If set to true, the positions of the quote characters are not considered part of the string.
 *                  In this case, the caret must be between the quotes to be considered inside the string.
 *                - If set to false, the positions of the quote characters enclosing a string are considered part of the string.
 *                  This means the caret is considered inside the string even if it's directly on one of the quotes.
 */
fun isInsideSingleQuotes(editor: VimEditor, pos: Int, isInner: Boolean): Boolean {
  val range = injector.psiService.getSingleQuotedString(editor, pos, isInner) ?: return false
  return pos in range
}

/**
 * Determines whether the specified caret position is within the bounds of a double-quoted string in the editor
 * (uses PSI when possible)
 *
 * @param isInner A flag indicating the behavior regarding quote characters themselves:
 *                - If set to true, the positions of the quote characters are not considered part of the string.
 *                  In this case, the caret must be between the quotes to be considered inside the string.
 *                - If set to false, the positions of the quote characters enclosing a string are considered part of the string.
 *                  This means the caret is considered inside the string even if it's directly on one of the quotes.
 */
fun isInsideDoubleQuotes(editor: VimEditor, pos: Int, isInner: Boolean): Boolean {
  val range = injector.psiService.getDoubleQuotedString(editor, pos, isInner) ?: return false
  return pos in range
}

/**
 * Determines whether the specified caret position is located within the bounds of a string in the document.
 * A "string" is defined as any sequence of text enclosed in single or double quotes.
 * (uses PSI when possible)
 *
 * @param isInner Flag indicating if the quotes themselves are treated as inside the string:
 *                - If true, the caret needs to be between the quote marks to be within the string.
 *                - If false, caret positions on the quote marks are counted as within the string.
 */
fun isInsideString(editor: VimEditor, pos: Int, isInner: Boolean): Boolean {
  val range = getStringAtPos(editor, pos, isInner) ?: return false
  return pos in range
}

/**
 * Retrieves the range of text that represents a "string" at a given caret position within the editor's document.
 * A "string" is defined as any sequence of text enclosed in single or double quotes.
 * (uses PSI when possible)
 *
 * @param isInner A flag indicating whether the start and end quote characters should be considered part of the string:
 *                - If set to true, only the text between the quote characters is included in the range.
 *                - If set to false, the quote characters at the boundaries are included as part of the string range.
 *
 * NOTE: Regardless of the [isInner] value, a TextRange will be returned if the caret is positioned on a quote character.
 */
fun getStringAtPos(editor: VimEditor, pos: Int, isInner: Boolean): TextRange? {
  return injector.psiService.getDoubleQuotedString(editor, pos, isInner) ?: injector.psiService.getSingleQuotedString(
    editor,
    pos,
    isInner
  )
}

/**
 * This method emulates the Vim '%' command for comments.
 * If the caret is positioned over a comment boundary, this method returns the position of the opposing boundary.
 */
fun getCommentsOppositeBoundary(editor: VimEditor, pos: Int): Int? {
  val (range, prefixToSuffix) = injector.psiService.getCommentAtPos(editor, pos) ?: return null
  if (prefixToSuffix == null) return null

  return if (pos < range.startOffset + prefixToSuffix.first.length) {
    range.endOffset - 1
  } else if (pos >= range.endOffset - prefixToSuffix.second.length) {
    range.startOffset
  } else {
    null
  }
}

/**
 * This looks on the current line, starting at the cursor position for one of {, }, (, ), [, or ]. It then searches
 * forward or backward, as appropriate for the associated match pair. String in double quotes are skipped over.
 * Single characters in single quotes are skipped too.
 *
 * @param editor The editor to search in
 * @return The offset within the editor of the found character or -1 if no match was found or none of the characters
 * were found on the remainder of the current line.
 */
//  TODO [vakhitov] it would be better to make this search PSI-aware and skip chars in strings and comments
fun findMatchingPairOnCurrentLine(editor: VimEditor, caret: ImmutableVimCaret): Int? {
  var pos = caret.offset

  val commentPos = getCommentsOppositeBoundary(editor, pos)
  if (commentPos != null) {
    return commentPos
  }

  val lineEnd = editor.getLineEndOffset(caret.getBufferPosition().line)

  // To handle the case where visual mode allows the user to go past the end of the line
  if (pos > 0 && pos >= lineEnd) {
    pos = lineEnd - 1
  }

  val chars = editor.text()
  val charPairs = parsMatchPairsOption(editor)

  val pairChars = charPairs.keys + charPairs.values
  if (!pairChars.contains(chars[pos])) {
    pos = chars.indexOfAnyOrNull(pairChars.toCharArray(), pos, lineEnd, null) ?: return null
  }

  val charToMatch = chars[pos]
  // TODO [vakhitov] should I implement BiMap for IdeaVim?
  val pairChar = charPairs[charToMatch] ?: charPairs.entries.first { it.value == charToMatch }.key
  val direction = if (charPairs.contains(charToMatch)) Direction.FORWARDS else Direction.BACKWARDS

  return findMatchingChar(editor, pos, charToMatch, pairChar, direction)
}

private fun parsMatchPairsOption(editor: VimEditor): Map<Char, Char> {
  return injector.options(editor).matchpairs
    .filter { it.length == 3 }
    .associate { it[0] to it[2] }
}

/**
 * Our implementation differs from the Vim one, but it is more consistent and uses the power of IDE.
 * We don't just count for opening and closing braces till their number will be equal, but keep context in mind.
 * If the first brace is inside string or comment, then the second one should be also in the same string or comment; otherwise there is no match.
 */
fun findMatchingChar(editor: VimEditor, start: Int, charToMatch: Char, pairChar: Char, direction: Direction): Int? {
  // If we are inside string, we search for the pair inside the string only
  val stringRange = getStringAtPos(editor, start, true)
  if (stringRange != null && start in stringRange) {
    return findBlockLocation(editor, stringRange, start, charToMatch, pairChar, direction)
  }

  val comment = injector.psiService.getCommentAtPos(editor, start)
  if (comment != null && start in comment.first) {
    val prefixToSuffix = comment.second
    return if (prefixToSuffix != null) {
      // If it is a block comment (has prefix & suffix), we search for the pair inside the block only
      findBlockLocation(editor, comment.first, start, charToMatch, pairChar, direction)
    } else {
      // If it is not a block comment, that there may be a sequence of single line comments, and we want to iterate over
      // all of them in an attempt to find a matching char
      val commentRange = getRangeOfNonBlockComments(editor, comment.first, direction)
      findBlockLocation(editor, commentRange, start, charToMatch, pairChar, direction)
    }
  }

  return findBlockLocation(editor, start, charToMatch, pairChar, direction, 0)
}

private fun getRangeOfNonBlockComments(editor: VimEditor, startComment: TextRange, direction: Direction): TextRange {
  var lastComment: TextRange = startComment

  while (true) {
    val nextNonWhitespaceChar = if (direction == Direction.FORWARDS) {
      findNextNonWhitespaceChar(editor.text(), lastComment.endOffset)
    } else {
      findPreviousNonWhitespaceChar(editor.text(), lastComment.startOffset - 1)
    } ?: break

    val nextComment = injector.psiService.getCommentAtPos(editor, nextNonWhitespaceChar)
    if (nextComment != null && nextComment.second == null) {
      lastComment = nextComment.first
    } else {
      break
    }
  }

  return if (direction == Direction.FORWARDS) {
    TextRange(startComment.startOffset, lastComment.endOffset)
  } else {
    TextRange(lastComment.startOffset, startComment.endOffset)
  }
}

private fun findNextNonWhitespaceChar(chars: CharSequence, startIndex: Int): Int? {
  for (i in startIndex..chars.lastIndex) {
    if (!chars[i].isWhitespace()) {
      return i
    }
  }
  return null
}

private fun findPreviousNonWhitespaceChar(chars: CharSequence, startIndex: Int): Int? {
  for (i in startIndex downTo 0) {
    if (!chars[i].isWhitespace()) {
      return i
    }
  }
  return null
}

/**
 * @see    `[{`, `]}` and similar Vim commands.
 * @return position of [count] unmatched [type]
 */
fun findUnmatchedBlock(editor: VimEditor, pos: Int, type: Char, count: Int): Int? {
  val chars: CharSequence = editor.text()

  val loc = BLOCK_CHARS.indexOf(type)
  val direction = if (loc % 2 == 0) Direction.BACKWARDS else Direction.FORWARDS

  val charToMatch = BLOCK_CHARS[loc]
  val pairChar = BLOCK_CHARS[loc - direction.toInt()]

  val start = if (pos < chars.length && (chars[pos] == type || chars[pos] == pairChar)) pos + direction.toInt() else pos
  return findBlockLocation(editor, start, charToMatch, pairChar, direction, count)
}

// TODO support delta & refactor me (mb use method below)
private fun findBlockLocation(
  editor: VimEditor,
  range: TextRange,
  start: Int,
  charToMatch: Char,
  pairChar: Char,
  direction: Direction,
  delta: Int = 0,
): Int? {
  val strictEscapeMatching = true // Vim's default behavior, see `help cpoptions`
  val chars = editor.text()

  var depth = 0
  val escapedRestriction = if (strictEscapeMatching) isEscaped(chars, start) else null
  var i: Int? = start
  while (i != null && i in range) {
    val c = chars[i]
    when (c) {
      charToMatch -> depth++
      pairChar -> depth--
    }
    if (depth == delta && (c == charToMatch || c == pairChar)) return i
    // TODO what should we do inside strings?
    i = chars.indexOfAnyOrNullInDirection(
      charArrayOf(charToMatch, pairChar),
      i + direction.toInt(),
      escapedRestriction,
      direction
    )
  }
  return null
}

private fun findBlockLocation(
  editor: VimEditor,
  start: Int,
  charToMatch: Char,
  pairChar: Char,
  direction: Direction,
  delta: Int,
  rangeToSearch: TextRange? = null,
): Int? {
  val strictEscapeMatching = true // Vim's default behavior, see `help cpoptions`
  val chars = editor.text()

  var result: Int? = null

  var depth = 0
  val escapedRestriction = if (strictEscapeMatching) isEscaped(chars, start) else null
  var i: Int? = start
  while (i != null && (rangeToSearch == null || rangeToSearch.contains(i))) {
    if (rangeToSearch == null) {
      val rangeToSkip = getStringAtPos(editor, i, false) ?: injector.psiService.getCommentAtPos(editor, i)?.first
      if (rangeToSkip != null) {
        val searchStart = if (direction == Direction.FORWARDS) rangeToSkip.endOffset else rangeToSkip.startOffset - 1
        i = chars.indexOfAnyOrNullInDirection(
          charArrayOf(charToMatch, pairChar),
          searchStart,
          escapedRestriction,
          direction
        )
        continue
      }
    }

    when (chars[i]) {
      charToMatch -> {
        depth++
        if (delta > 0) result =
          i // For `]}` and similar commands we should return the result even if [delta] is unachievable
      }

      pairChar -> {
        depth--
        if (delta < 0) result =
          i // For `]}` and similar commands we should return the result even if [delta] is unachievable
      }
    }

    if (depth == delta && (chars[i] == charToMatch || chars[i] == pairChar)) {
      result = i // For standard `%` motion, which should return [result] only if we succeeded to match
      break
    }

    i = chars.indexOfAnyOrNullInDirection(
      charArrayOf(charToMatch, pairChar),
      i + direction.toInt(),
      escapedRestriction,
      direction
    )
  }
  return result
}

/**
 * Find block enclosing the caret
 *
 * @param editor  The editor to search in
 * @param caret   The caret currently at
 * @param type    The type of block, e.g. (, [, {, <
 * @param count   Find the nth next occurrence of the block
 * @param isOuter Control whether the match includes block character
 * @return When the block is found, return text range matching where end offset is exclusive, otherwise return null
 */
// TODO please refactor me
fun findBlockRange(
  editor: VimEditor,
  caret: ImmutableVimCaret,
  type: Char,
  count: Int,
  isOuter: Boolean,
): TextRange? {
  val chars: CharSequence = editor.text()
  var pos: Int = caret.offset
  var start: Int = caret.selectionStart
  var end: Int = caret.selectionEnd

  val loc = BLOCK_CHARS.indexOf(type)
  val close = BLOCK_CHARS[loc + 1]

  // extend the range for blank line after type and before close, as they are excluded when inner match
  if (!isOuter) {
    if (start > 1 && chars[start - 2] == type && chars[start - 1] == '\n') {
      start--
    }
    if (end < chars.length && chars[end] == '\n') {
      var isSingleLineAllWhiteSpaceUntilClose = false
      var countWhiteSpaceCharacter = 1
      while (end + countWhiteSpaceCharacter < chars.length) {
        if (Character.isWhitespace(chars[end + countWhiteSpaceCharacter]) &&
          chars[end + countWhiteSpaceCharacter] != '\n'
        ) {
          countWhiteSpaceCharacter++
          continue
        }
        if (chars[end + countWhiteSpaceCharacter] == close) {
          isSingleLineAllWhiteSpaceUntilClose = true
        }
        break
      }
      if (isSingleLineAllWhiteSpaceUntilClose) {
        end += countWhiteSpaceCharacter
      }
    }
  }

  var rangeSelection = end - start > 1
  if (rangeSelection && start == 0) // early return not only for optimization
  {
    return null // but also not to break the interval semantic on this edge case (see below)
  }

  /* In case of successive inner selection. We want to break out of
   * the block delimiter of the current inner selection.
   * In other terms, for the rest of the algorithm, a previous inner selection of a block
   * if equivalent to an outer one. */

  /* In case of successive inner selection. We want to break out of
   * the block delimiter of the current inner selection.
   * In other terms, for the rest of the algorithm, a previous inner selection of a block
   * if equivalent to an outer one. */if (!isOuter && start - 1 >= 0 && type == chars[start - 1] && end < chars.length && close == chars[end]) {
    start -= 1
    pos = start
    rangeSelection = true
  }

  /* when one char is selected, we want to find the enclosing block of (start,end]
   * although when a range of characters is selected, we want the enclosing block of [start, end]
   * shifting the position allow to express which kind of interval we work on */

  /* when one char is selected, we want to find the enclosing block of (start,end]
   * although when a range of characters is selected, we want the enclosing block of [start, end]
   * shifting the position allow to express which kind of interval we work on */if (rangeSelection) pos =
    max(0.0, (start - 1).toDouble()).toInt()

  var (blockStart, blockEnd) = findBlock(editor, pos, type, close, count) ?: return null

  if (!isOuter) {
    blockStart++
    // exclude first line break after start for inner match
    if (chars[blockStart] == '\n') {
      blockStart++
    }
    val o = editor.getLineStartForOffset(blockEnd)
    var allWhite = true
    for (i in o until blockEnd) {
      if (!Character.isWhitespace(chars[i])) {
        allWhite = false
        break
      }
    }
    if (allWhite) {
      blockEnd = o - 2
    } else {
      blockEnd--
    }
  }

  // End offset exclusive
  return TextRange(blockStart, blockEnd + 1)
}

private fun findBlock(editor: VimEditor, pos: Int, charToMatch: Char, pairChar: Char, count: Int): Pair<Int, Int>? {
  val blockAtPos = getStringAtPos(editor, pos, true) ?: injector.psiService.getCommentAtPos(editor, pos)?.first

  var blockStart: Int?
  var blockEnd: Int?

  // There are three cases of "blocks" in Vim:
  //   1. There is a comment or string containing a pair of [charToMatch] to [pairChar],
  //   and caret is located between them,
  //   In this case match is checked only inside the same string or comment
  if (blockAtPos != null) {
    blockStart = findBlockLocation(
      editor,
      pos,
      charToMatch,
      pairChar,
      Direction.BACKWARDS,
      if (editor.text()[pos] == pairChar) count - 1 else count,
      blockAtPos
    )
    if (blockStart != null) {
      blockEnd = findBlockLocation(editor, blockStart, pairChar, charToMatch, Direction.FORWARDS, 0, blockAtPos)
      if (blockEnd != null && blockEnd >= pos) return blockStart to blockEnd
    }
  }

  //   2. Code contains [charToMatch] to [pairChar], and caret is located between them,
  //   In this case, all comments and strings are skipped and not included in count (see [findBlockLocation] implementation)
  blockStart = findBlockLocation(
    editor,
    pos,
    charToMatch,
    pairChar,
    Direction.BACKWARDS,
    if (editor.text()[pos] == pairChar) count - 1 else count
  )
  if (blockStart != null) {
    blockEnd = findBlockLocation(editor, blockStart, charToMatch, pairChar, Direction.FORWARDS, 0)
    if (blockEnd != null && blockEnd >= pos) return blockStart to blockEnd
  }

  if (count > 1) return null // There are no [charToMatch] to [pairChar] around caret at all

  //   3. There is a pair [charToMatch] to [pairChar] somewhere after the caret
  //   It may be found in both code or comment / string
  blockStart = editor.text().indexOfOrNull(charToMatch, pos)
  while (blockStart != null) {
    val containingBlock =
      getStringAtPos(editor, blockStart, false) ?: injector.psiService.getCommentAtPos(editor, blockStart)
    if (containingBlock != null) {
      blockEnd = findMatchingChar(editor, blockStart, charToMatch, pairChar, Direction.FORWARDS)
      if (blockEnd != null) return blockStart to blockEnd
      blockStart = editor.text().indexOfOrNull(charToMatch, blockStart + 1)
    } else {
      blockEnd = findMatchingChar(editor, blockStart, charToMatch, pairChar, Direction.FORWARDS)
      if (blockEnd != null) return blockStart to blockEnd
      break
    }
  }

  return null
}

private fun CharSequence.indexOfAnyOrNullInDirection(
  chars: CharArray,
  startIndex: Int,
  escaped: Boolean?,
  direction: Direction,
): Int? {
  return if (direction == Direction.FORWARDS) {
    this.indexOfAnyOrNull(chars, startIndex, length, escaped)
  } else {
    this.lastIndexOfAnyOrNull(chars, startIndex, -1, escaped)
  }
}

fun getDoubleQuotesRangeNoPSI(chars: CharSequence, currentPos: Int, isInner: Boolean): TextRange? =
  getQuoteRangeNoPSI(chars, currentPos, isInner, false)

fun getSingleQuotesRangeNoPSI(chars: CharSequence, currentPos: Int, isInner: Boolean): TextRange? =
  getQuoteRangeNoPSI(chars, currentPos, isInner, true)

private fun getQuoteRangeNoPSI(
  chars: CharSequence,
  currentPos: Int,
  isInner: Boolean,
  isSingleQuotes: Boolean,
): TextRange? {
  require(currentPos in 0..chars.lastIndex) // We can't use StrictMode here because I would like to test it without an injector initialized

  val start = chars.lastIndexOf('\n', currentPos) + 1
  val changes = quotesChanges(chars, start).takeWhileInclusive { it.position <= currentPos }

  val beforePos = changes.lastOrNull { it.position < currentPos }
  val atPos = changes.firstOrNull { it.position == currentPos }
  val afterPos = changes.firstOrNull { it.position > currentPos }

  val inQuoteCheck: (State) -> Boolean = if (isSingleQuotes) State::isInSingleQuotes else State::isInDoubleQuotes

  val openToClose: Pair<State, State> = if (beforePos != null && atPos != null && inQuoteCheck.invoke(beforePos)) {
    beforePos to atPos
  } else if (beforePos != null && afterPos != null && inQuoteCheck.invoke(beforePos)) {
    beforePos to afterPos
  } else if (atPos != null && afterPos != null && inQuoteCheck.invoke(atPos)) {
    atPos to afterPos
  } else {
    return null
  }

  return if (isInner) {
    TextRange(openToClose.first.position + 1, openToClose.second.position)
  } else {
    TextRange(openToClose.first.position, openToClose.second.position + 1)
  }
}

private fun isEscaped(chars: CharSequence, pos: Int): Boolean {
  var backslashCounter = 0

  var i = pos
  while (i-- > 0 && chars[i] == '\\') {
    backslashCounter++
  }
  return backslashCounter % 2 != 0
}

private data class State(val position: Int, val isInSingleQuotes: Boolean, val isInDoubleQuotes: Boolean)

private fun quotesChanges(chars: CharSequence, startIndex: Int) = sequence {
  var isInDoubleQuotes = false
  var isInSingleQuotes = false

  val eolIndex = chars.indexOfOrNull('\n', startIndex) ?: chars.length
  var nextQuoteIndex = chars.indexOfAnyOrNull(charArrayOf('"', '\''), startIndex, eolIndex, escaped = false)

  while (nextQuoteIndex != null) {
    val quotes = chars[nextQuoteIndex]
    when (quotes) {
      '"' -> {
        if (!isInSingleQuotes) {
          isInDoubleQuotes = !isInDoubleQuotes
          yield(State(nextQuoteIndex, false, isInDoubleQuotes))
        }
      }

      '\'' -> {
        if (!isInDoubleQuotes) {
          isInSingleQuotes = !isInSingleQuotes
          yield(State(nextQuoteIndex, isInSingleQuotes, false))
        }
      }
    }
    nextQuoteIndex = chars.indexOfAnyOrNull(charArrayOf('"', '\''), nextQuoteIndex + 1, eolIndex, escaped = false)
  }
}

private fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean) = sequence {
  with(iterator()) {
    while (hasNext()) {
      val next = next()
      yield(next)
      if (!predicate(next)) break
    }
  }
}
