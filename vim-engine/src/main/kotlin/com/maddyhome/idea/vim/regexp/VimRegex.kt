/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretListener
import com.maddyhome.idea.vim.api.VimDocument
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEditorBase
import com.maddyhome.idea.vim.api.VimFoldRegion
import com.maddyhome.idea.vim.api.VimIndentConfig
import com.maddyhome.idea.vim.api.VimScrollingModel
import com.maddyhome.idea.vim.api.VimSelectionModel
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.VirtualFile
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimEditorReplaceMask
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.regexp.engine.VimRegexEngine
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.DotMatcher
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.parser.CaseSensitivitySettings
import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.util.*

/**
 * Represents a compiled Vim pattern. Provides methods to
 * match, replace and split strings in the editor with a pattern.
 *
 * @see :help /pattern
 *
 */
class VimRegex(pattern: String) {
  /**
   * TODO: in my opinion only the find() and findAll() methods are necessary.
   *
   * The replace methods (not present here) should probably be implemented
   * somewhere else, using the find() or findAll() methods.
   *
   * The rest of the methods are just useless in my opinion
   */

  /**
   * Case sensitivity settings determined by the parser
   */
  private val caseSensitivitySettings: CaseSensitivitySettings

  /**
   * The NFA representing the compiled pattern
   */
  private val nfa: NFA

  /**
   * The NFA representing the compiled pattern, preceded by anything
   * Equivalent to ".*pattern"
   */
  private val nonExactNFA: NFA

  /**
   * Whether the pattern contains any upper case literal character
   */
  private val hasUpperCase: Boolean

  init {
    val parseResult = VimRegexParser.parse(pattern)

    when (parseResult) {
      is VimRegexParserResult.Failure -> throw VimRegexException(parseResult.errorCode.toString())
      is VimRegexParserResult.Success -> {
        nfa = PatternVisitor.visit(parseResult.tree)
        hasUpperCase = PatternVisitor.hasUpperCase
        nonExactNFA = NFA.fromMatcher(DotMatcher(false)).closure(false).concatenate(nfa)
        caseSensitivitySettings = parseResult.caseSensitivitySettings
      }
    }
  }

  /**
   * Indicates whether the pattern can find at least one match in the specified editor
   *
   * @param editor The editor where to look for the match in
   *
   * @return True if any match was found, false otherwise
   */
  fun containsMatchIn(
    editor: VimEditor,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Boolean {
    for (line in 0 until editor.lineCount()) {
      val result = simulateNonExactNFA(editor, editor.getLineStartOffset(line), options)
      if (result is VimMatchResult.Success) return true
    }

    /**
     * Entire editor was searched, but no match found
     */
    return false
  }

  internal fun containsMatchIn(
    text: String,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Boolean {
    return containsMatchIn(VimEditorWrapper(text), options)
  }

  /**
   * Returns the first match of a pattern in the editor, that comes after the startIndex
   *
   * Note that it is up to the caller to handle wrapscan. This is mainly so that the caller can notify the user.
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   *
   * @return The first match found in the editor after startIndex
   */
  fun findNext(
    editor: VimEditor,
    startIndex: Int = 0,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    /*
    if the startIndex is at the end of a line, start searching at the next position,
    to avoid the cursor getting stuck at line ends
    */
    val newStartIndex =
      if (startIndex + 1 == editor.getLineEndOffset(editor.offsetToBufferPosition(startIndex).line)) startIndex + 1
      else startIndex

    val lineStartIndex = editor.getLineStartOffset(editor.offsetToBufferPosition(newStartIndex).line)
    var index = lineStartIndex
    while (index <= editor.text().length) {
      val result = simulateNonExactNFA(editor, index, options)
      index = when (result) {
        is VimMatchResult.Success -> {
          if (result.range.startOffset > newStartIndex) {
            // The match comes after the startIndex, return it
            return result
          } else if (result.range.startOffset == startIndex && options.contains(VimRegexOptions.CAN_MATCH_START_LOCATION)) {
            // Accept a match at the current location. This means either we want to use the end position of a match, so
            // the current location is valid, or we've wrapped, and there's a match at index == 0.
            return result
          } else {
            // There is a match but starts before the startIndex, try again starting from the end of this match
            result.range.endOffset + if (result.range.startOffset == result.range.endOffset) 1 else 0
          }
        }
        // no match starting here, try the next line
        is VimMatchResult.Failure -> {
          val nextLine = editor.offsetToBufferPosition(index).line + 1
          if (nextLine >= editor.lineCount()) break
          editor.getLineStartOffset(nextLine)
        }
      }
    }
    // entire editor was searched, but no match found
    return VimMatchResult.Failure(VimRegexErrors.E486)
  }

  internal fun findNext(
    text: String,
    startIndex: Int = 0,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return findNext(VimEditorWrapper(text), startIndex, options)
  }

  /**
   * Returns the first match of a pattern in the editor, that comes before the startIndex
   *
   * Note that it is up to the caller to handle wrapscan. This is mainly so that the caller can notify the user.
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   *
   * @return The first match found in the editor before startIndex
   */
  fun findPrevious(
    editor: VimEditor,
    startIndex: Int = 0,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    val startLine = editor.offsetToBufferPosition(startIndex).line
    val result = findLastMatchInLine(editor, startLine, startIndex - 1, options)
    if (result is VimMatchResult.Success && result.range.startOffset < startIndex) {
      // there is a match at this line that starts before the startIndex
      return result
    } else {
      // try searching in previous lines, line by line until the start of the buffer
      for (currentLine in startLine - 1 downTo 0) {
        val previous = findLastMatchInLine(editor, currentLine, options = options)
        if (previous is VimMatchResult.Success) return previous
      }
      // there are no matches in the entire file
      return VimMatchResult.Failure(VimRegexErrors.E486)
    }
  }

  internal fun findPrevious(
    text: String,
    startIndex: Int = 0,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return findPrevious(VimEditorWrapper(text), startIndex, options)
  }

  /**
   * Finds the last match that starts at line, before maxIndex
   *
   * @param editor The editor where to look for the match in
   * @param line   The where the match should start
   * @param maxIndex The maximum index (exclusive) where the match should start
   *
   * @return The last match found, if any
   */
  private fun findLastMatchInLine(
    editor: VimEditor,
    line: Int,
    maxIndex: Int = editor.getLineEndOffset(line),
    options: EnumSet<VimRegexOptions>,
  ): VimMatchResult {
    var index = editor.getLineStartOffset(line)
    var prevResult: VimMatchResult = VimMatchResult.Failure(VimRegexErrors.E486)
    val returnEndPosition = options.contains(VimRegexOptions.WANT_END_POSITION)
    while (index <= maxIndex) {
      val result = simulateNonExactNFA(editor, index, options)
      when (result) {
        // no more matches in this line, break out of the loop
        is VimMatchResult.Failure -> break
        is VimMatchResult.Success -> {
          // no more relevant matches in this line, break out of the loop
          if ((!returnEndPosition && result.range.startOffset > maxIndex) || (returnEndPosition && result.range.endOffset > maxIndex)) break

          // match found, try to find more after it
          prevResult = result
          index =
            if (result.range.startOffset == result.range.endOffset) result.range.endOffset + 1 else result.range.endOffset
        }
      }
    }
    // return the last found match in the line, if any
    return prevResult
  }

  /**
   * Returns a sequence of all occurrences of a pattern within
   * the editor, beginning at the specified index
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   *
   * @return All the matches found in the editor
   */
  fun findAll(
    editor: VimEditor,
    startIndex: Int = 0,
    maxIndex: Int = editor.text().length,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): List<VimMatchResult.Success> {
    var index = startIndex
    val foundMatches: MutableList<VimMatchResult.Success> = emptyList<VimMatchResult.Success>().toMutableList()
    while (index < maxIndex) {
      val result = simulateNonExactNFA(editor, index, options)
      when (result) {
        /**
         * A match was found, add it to foundMatches and increment
         * next index accordingly
         */
        is VimMatchResult.Success -> {
          foundMatches.add(result)
          index = if (result.range.startOffset == result.range.endOffset) result.range.endOffset + 1
          else result.range.endOffset
        }

        /**
         * No match found starting on this index, try searching on next line
         */
        is VimMatchResult.Failure -> {
          val nextLine = editor.offsetToBufferPosition(index).line + 1
          if (nextLine >= editor.lineCount()) break
          index = editor.getLineStartOffset(nextLine)
        }
      }
    }
    return foundMatches
  }

  internal fun findAll(
    text: String,
    startIndex: Int = 0,
    maxIndex: Int = text.length,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): List<VimMatchResult.Success> {
    return findAll(VimEditorWrapper(text), startIndex, maxIndex, options)
  }

  /**
   * Searches for a match of a pattern on a give line, starting at a certain column.
   *
   * @param editor     The editor where to look for the match in
   * @param line       The number of the line where to look for the match in
   * @param column     The column of that line where to start looking for a match
   */
  fun findInLine(
    editor: VimEditor,
    line: Int,
    column: Int = 0,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return simulateNonExactNFA(editor, editor.getLineStartOffset(line) + column, options)
  }

  internal fun findInLine(
    text: String,
    line: Int,
    column: Int = 0,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return findInLine(VimEditorWrapper(text), line, column, options)
  }

  /**
   * "Simulates" the substitution of the match of a pattern with a substitution string.
   *
   * Substitution is not actually performed here, only simulated, since it may still be pending
   * confirmation from the user.
   *
   * @param editor               The editor where to look for the match and perform the substitution in
   * @param substituteString     The string used for substitution. Can either be taken literally or contain characters with a special meaning
   * @param lastSubstituteString The substitution string lastly used.
   * @param line                 The line to simulate the substitution in
   * @param column               The column of that line where to start looking for a match
   * @param takeLiterally        Whether to always take the string literally, meaning no character has a special meaning
   */
  fun substitute(
    editor: VimEditor,
    substituteString: String,
    lastSubstituteString: String,
    line: Int,
    column: Int = 0,
    takeLiterally: Boolean = false,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Pair<VimMatchResult.Success, String>? {
    val match = findInLine(editor, line, column, options)
    return when (match) {
      is VimMatchResult.Failure -> null
      is VimMatchResult.Success -> Pair(
        match,
        if (takeLiterally) substituteString else buildSubstituteString(match, substituteString, lastSubstituteString)
      )
    }
  }

  private fun buildSubstituteString(
    matchResult: VimMatchResult.Success,
    substituteString: String,
    lastSubstituteString: String,
    magic: Boolean = true,
  ): String {
    val result = StringBuilder()
    var caseSettings: SubstituteCase = SubstituteCase.DEFAULT

    var index = 0
    while (index < substituteString.length) {
      if (substituteString[index] == '\\') {
        index++
        if (index >= substituteString.length) {
          result.append('\\')
          break
        }
        when (substituteString[index]) {
          '&' -> result.append(if (magic) '&' else matchResult.value)
          '~' -> result.append(if (magic) '~' else buildSubstituteString(matchResult, lastSubstituteString, "", false))
          '0' -> result.append(matchResult.value)
          '1' -> result.append(matchResult.groups.get(1)?.value ?: "")
          '2' -> result.append(matchResult.groups.get(2)?.value ?: "")
          '3' -> result.append(matchResult.groups.get(3)?.value ?: "")
          '4' -> result.append(matchResult.groups.get(4)?.value ?: "")
          '5' -> result.append(matchResult.groups.get(5)?.value ?: "")
          '6' -> result.append(matchResult.groups.get(6)?.value ?: "")
          '7' -> result.append(matchResult.groups.get(7)?.value ?: "")
          '8' -> result.append(matchResult.groups.get(8)?.value ?: "")
          '9' -> result.append(matchResult.groups.get(9)?.value ?: "")
          'u' -> caseSettings = SubstituteCase.UPPER
          'U' -> caseSettings = SubstituteCase.UPPER_PERSISTENT
          'l' -> caseSettings = SubstituteCase.LOWER
          'L' -> caseSettings = SubstituteCase.LOWER_PERSISTENT
          'e' -> caseSettings = SubstituteCase.DEFAULT
          'E' -> caseSettings = SubstituteCase.DEFAULT
          'r' -> result.append('\n')
          'n' -> result.append('\u0000')
          'b' -> result.append('\b')
          't' -> result.append('\t')
          '\\' -> result.append('\\')
          else -> {
            val buildResult = buildLiteralChar(substituteString[index], caseSettings)
            caseSettings = buildResult.second
            result.append(buildResult.first)
          }
        }
      } else if (substituteString[index] == '&' && magic) {
        result.append(matchResult.value)
      } else if (substituteString[index] == '~' && magic) {
        result.append(buildSubstituteString(matchResult, lastSubstituteString, "", true))
      } else {
        val buildResult = buildLiteralChar(substituteString[index], caseSettings)
        caseSettings = buildResult.second
        result.append(buildResult.first)
      }
      index++
    }

    return result.toString()
  }

  private fun buildLiteralChar(
    char: Char,
    caseSettings: SubstituteCase,
  ): Pair<Char, SubstituteCase> {
    return when (caseSettings) {
      SubstituteCase.DEFAULT -> Pair(char, caseSettings)
      SubstituteCase.UPPER -> Pair(char.uppercaseChar(), SubstituteCase.DEFAULT)
      SubstituteCase.UPPER_PERSISTENT -> Pair(char.uppercaseChar(), caseSettings)
      SubstituteCase.LOWER -> Pair(char.lowercaseChar(), SubstituteCase.DEFAULT)
      SubstituteCase.LOWER_PERSISTENT -> Pair(char.lowercaseChar(), caseSettings)
    }
  }

  private enum class SubstituteCase {
    DEFAULT,
    UPPER,
    UPPER_PERSISTENT,
    LOWER,
    LOWER_PERSISTENT,
  }

  /**
   * Attempts to match a pattern exactly at the specified
   * index in the editor text.
   *
   * @param editor The editor where to look for the match in
   * @param index  The index to start the match
   *
   * @return The match, either successful or not, found at the specified index
   */
  fun matchAt(
    editor: VimEditor,
    index: Int,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return simulateNFA(editor, index, options)
  }

  internal fun matchAt(
    text: String,
    index: Int,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return matchAt(VimEditorWrapper(text), index, options)
  }

  /**
   * Attempts to match the entire editor against the pattern.
   *
   * @param editor The editor where to look for the match in
   *
   * @return The match, either successful or not, when matching against the entire editor
   */
  fun matchEntire(
    editor: VimEditor,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    val result = simulateNFA(editor, options = options)
    return when (result) {
      is VimMatchResult.Failure -> result
      is VimMatchResult.Success -> {
        if (result.range.endOffset == editor.text().length) result
        else VimMatchResult.Failure(VimRegexErrors.E486) // create a more appropriate error code?
      }
    }
  }

  internal fun matchEntire(
    text: String,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): VimMatchResult {
    return matchEntire(VimEditorWrapper(text), options)
  }

  /**
   * Indicates whether the pattern matches the entire editor.
   *
   * @param editor The editor where to look for the match in
   *
   * @return True if the entire editor matches, false otherwise
   */
  fun matches(
    editor: VimEditor,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Boolean {
    val result = simulateNFA(editor, options = options)
    return when (result) {
      is VimMatchResult.Failure -> false
      is VimMatchResult.Success -> result.range.endOffset == editor.text().length
    }
  }

  fun matches(
    text: String,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Boolean {
    return matches(VimEditorWrapper(text), options)
  }

  /**
   * Checks if a pattern matches a part of the editor
   * starting exactly at the specified index.
   *
   * @param editor The editor where to look for the match in
   *
   * @return True if there is a successful match starting at the specified index, false otherwise
   */
  fun matchesAt(
    editor: VimEditor,
    index: Int,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Boolean {
    return when (simulateNFA(editor, index, options)) {
      is VimMatchResult.Success -> true
      is VimMatchResult.Failure -> false
    }
  }

  internal fun matchesAt(
    text: String,
    index: Int,
    options: EnumSet<VimRegexOptions> = noneOfEnum(),
  ): Boolean {
    return matchesAt(VimEditorWrapper(text), index, options)
  }

  /**
   * Simulates the internal NFA with the determined flags,
   * started on a given index.
   *
   * @param editor The editor that is used for the simulation
   * @param index  The index where the simulation should start
   *
   * @return The resulting match result
   */
  private fun simulateNFA(editor: VimEditor, index: Int = 0, options: EnumSet<VimRegexOptions>): VimMatchResult {
    return VimRegexEngine.simulate(nfa, editor, index, shouldIgnoreCase(options))
  }

  /**
   * Simulates the internal non-exact NFA with the determined flags,
   * started on a given index.
   *
   * @param editor The editor that is used for the simulation
   * @param index  The index where the simulation should start
   *
   * @return The resulting match result
   */
  private fun simulateNonExactNFA(
    editor: VimEditor,
    index: Int = 0,
    options: EnumSet<VimRegexOptions>,
  ): VimMatchResult {
    return VimRegexEngine.simulate(nonExactNFA, editor, index, shouldIgnoreCase(options))
  }

  /**
   * Determines, based on information that comes from the parser and other
   * options that may be set, whether to ignore case.
   */
  private fun shouldIgnoreCase(options: EnumSet<VimRegexOptions>): Boolean {
    return when (caseSensitivitySettings) {
      CaseSensitivitySettings.NO_IGNORE_CASE -> false
      CaseSensitivitySettings.IGNORE_CASE -> true
      CaseSensitivitySettings.DEFAULT -> options.contains(VimRegexOptions.IGNORE_CASE) && !(options.contains(
        VimRegexOptions.SMART_CASE
      ) && hasUpperCase)
    }
  }

  private class VimEditorWrapper(private val text: String) : VimEditorBase() {
    override fun updateMode(mode: Mode) {
      TODO("Not yet implemented")
    }

    override fun updateIsReplaceCharacter(isReplaceCharacter: Boolean) {
      TODO("Not yet implemented")
    }

    override val lfMakesNewLine: Boolean = true
    override var vimChangeActionSwitchMode: Mode? = null
    override val indentConfig: VimIndentConfig
      get() = TODO("Not yet implemented")
    override var replaceMask: VimEditorReplaceMask? = null

    override fun fileSize(): Long {
      return text.length.toLong()
    }

    override fun text(): CharSequence = text

    override fun nativeLineCount(): Int {
      return text.count { it == '\n' } + 1
    }

    override fun getLineRange(line: Int): Pair<Int, Int> {
      return getLineStartOffset(line) to getLineEndOffset(line)
    }

    override fun carets(): List<VimCaret> = emptyList()

    override fun nativeCarets(): List<VimCaret> = emptyList()

    override fun forEachCaret(action: (VimCaret) -> Unit) {}

    override fun forEachNativeCaret(action: (VimCaret) -> Unit, reverse: Boolean) {}

    override fun isInForEachCaretScope(): Boolean = false

    override fun primaryCaret(): VimCaret {
      throw ExException("No caret present")
    }

    override fun currentCaret(): VimCaret {
      throw ExException("No caret present")
    }

    override fun isWritable(): Boolean = false

    override fun isDocumentWritable(): Boolean = false

    override fun isOneLineMode(): Boolean = false

    override fun search(
      pair: Pair<Int, Int>,
      editor: VimEditor,
      shiftType: LineDeleteShift,
    ): Pair<Pair<Int, Int>, LineDeleteShift>? {
      TODO("Not yet implemented")
    }

    override fun offsetToBufferPosition(offset: Int): BufferPosition {
      if (offset < 0 || offset > text.length) return BufferPosition(-1, -1)

      var line = 0
      var lastLineStart = 0

      for (i in 0 until offset) {
        if (text[i] == '\n') {
          line++
          lastLineStart = i + 1
        }
      }

      val column = offset - lastLineStart
      return BufferPosition(line, column)
    }

    override fun bufferPositionToOffset(position: BufferPosition): Int {
      val lines = text.lines()
      var offset = 0
      for (i in 0 until position.line) {
        offset += lines[i].length + 1
      }
      offset += position.column
      return offset
    }

    override fun offsetToVisualPosition(offset: Int): VimVisualPosition {
      return bufferPositionToVisualPosition(offsetToBufferPosition(offset))
    }

    override fun visualPositionToOffset(position: VimVisualPosition): Int {
      return bufferPositionToOffset(visualPositionToBufferPosition(position))
    }

    override fun visualPositionToBufferPosition(position: VimVisualPosition): BufferPosition {
      return BufferPosition(position.line, position.column, position.leansRight)
    }

    override fun bufferPositionToVisualPosition(position: BufferPosition): VimVisualPosition {
      return VimVisualPosition(position.line, position.column, position.leansForward)
    }

    override fun getVirtualFile(): VirtualFile? = null

    override fun deleteString(range: TextRange) {}

    override fun getSelectionModel(): VimSelectionModel {
      TODO("Not yet implemented")
    }

    override fun getScrollingModel(): VimScrollingModel {
      TODO("Not yet implemented")
    }

    override fun removeCaret(caret: VimCaret) {
    }

    override fun removeSecondaryCarets() {
    }

    override fun vimSetSystemBlockSelectionSilently(start: BufferPosition, end: BufferPosition) {
    }

    override fun getLineStartOffset(line: Int): Int {
      if (line < 0) return -1
      var currentLine = 0
      for (index in text.indices) {
        if (currentLine == line) return index
        if (text[index] == '\n') currentLine++
      }
      return if (line == 0) 0 else -1
    }

    override fun getLineEndOffset(line: Int): Int {
      if (line < 0) return -1
      var currentLine = 0
      for (index in text.indices) {
        if (text[index] == '\n') {
          if (currentLine == line) return index - 1
          currentLine++
        }
      }
      return if (line == currentLine) text.length - 1 else -1
    }

    override fun addCaretListener(listener: VimCaretListener) {}

    override fun removeCaretListener(listener: VimCaretListener) {}

    override fun isDisposed(): Boolean = false

    override fun removeSelection() {}

    override fun getPath(): String? = null

    override fun extractProtocol(): String? = null

    override val projectId: String = "no project, I am just a piece of text wrapped into an Enditor for Regexp to work"

    override fun exitInsertMode(context: ExecutionContext) {}

    override fun exitSelectModeNative(adjustCaret: Boolean) {}

    override var vimLastSelectionType: SelectionType? = null

    override fun isTemplateActive(): Boolean = false

    override fun startGuardedBlockChecking() {}
    override fun stopGuardedBlockChecking() {}

    override fun hasUnsavedChanges(): Boolean = false

    override fun getLastVisualLineColumnNumber(line: Int): Int {
      TODO("Not yet implemented")
    }

    override fun createLiveMarker(start: Int, end: Int): LiveRange {
      TODO("Not yet implemented")
    }

    override var insertMode: Boolean = false
    override val document: VimDocument
      get() = TODO("Not yet implemented")

    override fun createIndentBySize(size: Int): String {
      TODO("Not yet implemented")
    }

    override fun getFoldRegionAtOffset(offset: Int): VimFoldRegion? {
      return null
    }

    override fun <T : ImmutableVimCaret> findLastVersionOfCaret(caret: T): T? {
      return null
    }
  }
}
