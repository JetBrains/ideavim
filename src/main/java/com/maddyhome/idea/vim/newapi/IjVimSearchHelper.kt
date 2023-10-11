/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchHelperBase
import com.maddyhome.idea.vim.api.anyNonWhitespace
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.checkInString
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.state.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import java.util.*
import java.util.function.Function
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.max

@Service
internal class IjVimSearchHelper : VimSearchHelperBase() {

  companion object {
    private const val BLOCK_CHARS = "{}()[]<>"
    private val logger = Logger.getInstance(IjVimSearchHelper::class.java.name)
  }
  override fun findSection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    direction: Int,
    count: Int,
  )
  : Int {
    val documentText: CharSequence = editor.ij.document.charsSequence
    var currentLine: Int = caret.ij.logicalPosition.line + direction
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

  override fun findMethodEnd(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return PsiHelper.findMethodEnd(editor.ij, caret.ij.offset, count)
  }

  override fun findMethodStart(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return PsiHelper.findMethodStart(editor.ij, caret.ij.offset, count)
  }

  override fun findUnmatchedBlock(editor: VimEditor, caret: ImmutableVimCaret, type: Char, count: Int): Int {
    val chars: CharSequence = editor.ij.document.charsSequence
    var pos: Int = caret.ij.offset
    val loc = BLOCK_CHARS.indexOf(type)
    // What direction should we go now (-1 is backward, 1 is forward)
    val dir = if (loc % 2 == 0) Direction.BACKWARDS else Direction.FORWARDS
    // Which character did we find and which should we now search for
    val match = BLOCK_CHARS[loc]
    val found = BLOCK_CHARS[loc - dir.toInt()]

    if (pos < chars.length && chars[pos] == type) {
      pos += dir.toInt()
    }
    return findBlockLocation(chars, found, match, dir, pos, count)
  }

  private fun findBlockLocation(
    chars: CharSequence,
    found: Char,
    match: Char,
    dir: Direction,
    pos: Int,
    cnt: Int,
  ): Int {
    var position = pos
    var count = cnt
    var res = -1
    val initialPos = position
    val initialInString = checkInString(chars, position, true)
    val inCheckPosF =
      Function { x: Int -> if (dir === Direction.BACKWARDS && x > 0) x - 1 else x + 1 }
    val inCheckPos = inCheckPosF.apply(position)
    var inString = checkInString(chars, inCheckPos, true)
    var inChar = checkInString(chars, inCheckPos, false)
    var stack = 0
    // Search to start or end of file, as appropriate
    val charsToSearch: Set<Char> = HashSet(listOf('\'', '"', '\n', match, found))
    while (position >= 0 && position < chars.length && count > 0) {
      val (c, second) = SearchHelper.findPositionOfFirstCharacter(chars, position, charsToSearch, true, dir) ?: return -1
      position = second
      // If we found a match and we're not in a string...
      if (c == match && (!inString) && !inChar) {
        // We found our match
        if (stack == 0) {
          res = position
          count--
        } else {
          stack--
        }
      } else if (c == '\n') {
        inString = false
        inChar = false
      } else if (position != initialPos) {
        // We found another character like our original - belongs to another pair
        if (!inString && !inChar && c == found) {
          stack++
        } else if (!inChar) {
          inString = checkInString(chars, inCheckPosF.apply(position), true)
        } else if (!inString) {
          inChar = checkInString(chars, inCheckPosF.apply(position), false)
        }
      }
      position += dir.toInt()
    }
    return res
  }

  override fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange? {
    return if (injector.globalIjOptions().useNewRegex) super.findPattern(editor, pattern, startOffset, count, searchOptions)
    else SearchHelper.findPattern(editor.ij, pattern, startOffset, count, searchOptions)
  }

  override fun findAll(
    editor: VimEditor,
    pattern: String,
    startLine: Int,
    endLine: Int,
    ignoreCase: Boolean,
  ): List<TextRange> {
    return if (injector.globalIjOptions().useNewRegex) super.findAll(editor, pattern, startLine, endLine, ignoreCase)
    else SearchHelper.findAll(editor.ij, pattern, startLine, endLine, ignoreCase)
  }

  override fun findNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int {
    val line: Int = caret.ij.logicalPosition.line
    val start = editor.getLineStartOffset(line)
    val end = editor.getLineEndOffset(line, true)
    val chars: CharSequence = editor.ij.document.charsSequence
    var found = 0
    val step = if (count >= 0) 1 else -1
    var pos: Int = caret.ij.offset + step
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

  override fun findWordUnderCursor(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    dir: Int,
    isOuter: Boolean,
    isBig: Boolean,
    hasSelection: Boolean,
  ): TextRange {
    if (logger.isDebugEnabled) {
      logger.debug("count=$count")
      logger.debug("dir=$dir")
      logger.debug("isOuter=$isOuter")
      logger.debug("isBig=$isBig")
      logger.debug("hasSelection=$hasSelection")
    }

    val chars: CharSequence = editor.ij.document.charsSequence
    //int min = EditorHelper.getLineStartOffset(editor, EditorHelper.getCurrentLogicalLine(editor));
    //int max = EditorHelper.getLineEndOffset(editor, EditorHelper.getCurrentLogicalLine(editor), true);
    val min = 0
    val max: Int = editor.ij.fileSize
    if (max == 0) return TextRange(0, 0)

    if (logger.isDebugEnabled) {
      logger.debug("min=$min")
      logger.debug("max=$max")
    }

    val pos: Int = caret.ij.offset
    if (chars.length <= pos) return TextRange(chars.length - 1, chars.length - 1)

    val startSpace = charType(editor, chars[pos], isBig) === CharacterHelper.CharacterType.WHITESPACE
    // Find word start
    val onWordStart = pos == min ||
      charType(editor, chars[pos - 1], isBig) !==
      charType(editor, chars[pos], isBig)
    var start = pos

    if (logger.isDebugEnabled) {
      logger.debug("pos=$pos")
      logger.debug("onWordStart=$onWordStart")
    }

    if (!onWordStart && !(startSpace && isOuter) || hasSelection || count > 1 && dir == -1) {
      start = if (dir == 1) {
        findNextWord(editor, pos, -1, isBig, !isOuter)
      } else {
        findNextWord(
          editor,
          pos,
          -(count - if (onWordStart && !hasSelection) 1 else 0),
          isBig,
          !isOuter
        )
      }
      start = editor.normalizeOffset(start, false)
    }

    if (logger.isDebugEnabled) logger.debug("start=$start")

    // Find word end

    // Find word end
    val onWordEnd = pos >= max - 1 ||
      charType(editor, chars[pos + 1], isBig) !==
      charType(editor, chars[pos], isBig)

    if (logger.isDebugEnabled) logger.debug("onWordEnd=$onWordEnd")

    var end = pos
    if (!onWordEnd || hasSelection || count > 1 && dir == 1 || startSpace && isOuter) {
      end = if (dir == 1) {
        val c = count - if (onWordEnd && !hasSelection && (!(startSpace && isOuter) || startSpace && !isOuter)) 1 else 0
        findNextWordEnd(editor, pos, c, isBig, !isOuter)
      } else {
        findNextWordEnd(editor, pos, 1, isBig, !isOuter)
      }
    }

    if (logger.isDebugEnabled) logger.debug("end=$end")

    var goBack = startSpace && !hasSelection || !startSpace && hasSelection && !onWordStart
    if (dir == 1 && isOuter) {
      var firstEnd = end
      if (count > 1) {
        firstEnd = findNextWordEnd(editor, pos, 1, isBig, false)
      }
      if (firstEnd < max - 1) {
        if (charType(editor, chars[firstEnd + 1], false) !== CharacterHelper.CharacterType.WHITESPACE) {
          goBack = true
        }
      }
    }
    if (dir == -1 && isOuter && startSpace) {
      if (pos > min) {
        if (charType(editor, chars[pos - 1], false) !== CharacterHelper.CharacterType.WHITESPACE) {
          goBack = true
        }
      }
    }

    var goForward = dir == 1 && isOuter && (!startSpace && !onWordEnd || startSpace && onWordEnd && hasSelection)
    if (!goForward && dir == 1 && isOuter) {
      var firstEnd = end
      if (count > 1) {
        firstEnd = findNextWordEnd(editor, pos, 1, isBig, false)
      }
      if (firstEnd < max - 1) {
        if (charType(editor, chars[firstEnd + 1], false) !== CharacterHelper.CharacterType.WHITESPACE) {
          goForward = true
        }
      }
    }
    if (!goForward && dir == 1 && isOuter && !startSpace && !hasSelection) {
      if (end < max - 1) {
        if (charType(editor, chars[end + 1], !isBig) !==
          charType(editor, chars[end], !isBig)
        ) {
          goForward = true
        }
      }
    }

    if (logger.isDebugEnabled) {
      logger.debug("goBack=$goBack")
      logger.debug("goForward=$goForward")
    }

    if (goForward) {
      if (editor.anyNonWhitespace(end, 1)) {
        while (end + 1 < max &&
          charType(editor, chars[end + 1], false) === CharacterHelper.CharacterType.WHITESPACE
        ) {
          end++
        }
      }
    }
    if (goBack) {
      if (editor.anyNonWhitespace(start, -1)) {
        while (start > min &&
          charType(editor, chars[start - 1], false) === CharacterHelper.CharacterType.WHITESPACE
        ) {
          start--
        }
      }
    }

    if (logger.isDebugEnabled) {
      logger.debug("start=$start")
      logger.debug("end=$end")
    }

    // End offset is exclusive
    return TextRange(start, end + 1)
  }

  override fun findBlockTagRange(editor: VimEditor, caret: ImmutableVimCaret, count: Int, isOuter: Boolean): TextRange? {
    var counter = count
    var isOuterVariable = isOuter
    val position: Int = caret.ij.offset
    val sequence: CharSequence = editor.ij.document.charsSequence

    val selectionStart: Int = caret.ij.selectionStart
    val selectionEnd: Int = caret.ij.selectionEnd

    val isRangeSelection = selectionEnd - selectionStart > 1

    var searchStartPosition: Int
    searchStartPosition = if (!isRangeSelection) {
      val line: Int = caret.ij.logicalPosition.line
      val lineBegin: Int = editor.ij.document.getLineStartOffset(line)
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
      val mode = getInstance(editor).mode
      if (mode is VISUAL) {
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

  override fun findBlockRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
    val chars: CharSequence = editor.ij.document.charsSequence
    var pos: Int = caret.ij.offset
    var start: Int = caret.ij.selectionStart
    var end: Int = caret.ij.selectionEnd

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

    val initialPosIsInString = checkInString(chars, pos, true)

    var bstart = -1
    var bend = -1

    var startPosInStringFound = false

    if (initialPosIsInString) {
      val quoteRange = injector.searchHelper
        .findBlockQuoteInLineRange(editor, caret, '"', false)
      if (quoteRange != null) {
        val startOffset = quoteRange.startOffset
        val endOffset = quoteRange.endOffset
        val subSequence = chars.subSequence(startOffset, endOffset)
        val inQuotePos = pos - startOffset
        var inQuoteStart =
          findBlockLocation(subSequence, close, type, Direction.BACKWARDS, inQuotePos, count)
        if (inQuoteStart == -1) {
          inQuoteStart =
            findBlockLocation(subSequence, close, type, Direction.FORWARDS, inQuotePos, count)
        }
        if (inQuoteStart != -1) {
          startPosInStringFound = true
          val inQuoteEnd =
            findBlockLocation(subSequence, type, close, Direction.FORWARDS, inQuoteStart, 1)
          if (inQuoteEnd != -1) {
            bstart = inQuoteStart + startOffset
            bend = inQuoteEnd + startOffset
          }
        }
      }
    }

    if (!startPosInStringFound) {
      bstart = findBlockLocation(chars, close, type, Direction.BACKWARDS, pos, count)
      if (bstart == -1) {
        bstart = findBlockLocation(chars, close, type, Direction.FORWARDS, pos, count)
      }
      if (bstart != -1) {
        bend = findBlockLocation(chars, type, close, Direction.FORWARDS, bstart, 1)
      }
    }

    if (bstart == -1 || bend == -1) {
      return null
    }

    if (!isOuter) {
      bstart++
      // exclude first line break after start for inner match
      if (chars[bstart] == '\n') {
        bstart++
      }
      val o = editor.getLineStartForOffset(bend)
      var allWhite = true
      for (i in o until bend) {
        if (!Character.isWhitespace(chars[i])) {
          allWhite = false
          break
        }
      }
      if (allWhite) {
        bend = o - 2
      } else {
        bend--
      }
    }

    // End offset exclusive
    return TextRange(bstart, bend + 1)
  }
}
