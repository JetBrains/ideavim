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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.state.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import it.unimi.dsi.fastutil.ints.IntComparator
import it.unimi.dsi.fastutil.ints.IntComparators
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

@Service
internal class IjVimSearchHelper : VimSearchHelperBase() {

  companion object {
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

  override fun findMisspelledWord(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    val startOffset: Int
    val endOffset: Int
    val skipCount: Int
    val offsetOrdering: IntComparator

    if (count < 0) {
      startOffset = 0
      endOffset = caret.offset.point - 1
      skipCount = -count - 1
      offsetOrdering = IntComparators.OPPOSITE_COMPARATOR
    }
    else {
      startOffset = caret.offset.point + 1
      endOffset = editor.ij.document.textLength
      skipCount = count - 1
      offsetOrdering = IntComparators.NATURAL_COMPARATOR
    }

    return SearchHelper.findMisspelledWords(editor.ij, startOffset, endOffset, skipCount, offsetOrdering)
  }
}
