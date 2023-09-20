/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.engine.VimRegexEngine
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.DotMatcher
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.parser.CaseSensitivitySettings
import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor

/**
 * Represents a compiled Vim pattern. Provides methods to
 * match, replace and split strings in the editor with a pattern.
 *
 * @see :help /pattern
 *
 */
public class VimRegex(pattern: String) {
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

  init {
    val parseResult = VimRegexParser.parse(pattern)

    when (parseResult) {
      is VimRegexParserResult.Failure -> throw VimRegexException(parseResult.errorCode.toString())
      is VimRegexParserResult.Success -> {
        nfa = PatternVisitor.visit(parseResult.tree)
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
  public fun containsMatchIn(
    editor: VimEditor,
    options: List<VimRegexOptions> = emptyList()
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

  /**
   * Returns the first match of a pattern in the editor, that comes after the startIndex
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   *
   * @return The first match found in the editor after startIndex
   */
  public fun findNext(
    editor: VimEditor,
    startIndex: Int = 0,
    options: List<VimRegexOptions> = emptyList()
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
          // the match comes after the startIndex, return it
          if (result.range.startOffset > newStartIndex) return result
          // there is a match but starts before the startIndex, try again starting from the end of this match
          else result.range.endOffset + if (result.range.startOffset == result.range.endOffset) 1 else 0
        }
        // no match starting here, try the next line
        is VimMatchResult.Failure -> {
          val nextLine = editor.offsetToBufferPosition(index).line + 1
          if (nextLine >= editor.lineCount()) break
          editor.getLineStartOffset(nextLine)
        }
      }
    }
    index = 0
    // no match found after startIndex, try wrapping around to file start
    while (index <= startIndex) {
      val result = simulateNonExactNFA(editor, index, options)
      // just return the first match found
      when (result) {
        is VimMatchResult.Success -> return result
        is VimMatchResult.Failure -> {
          val nextLine = editor.offsetToBufferPosition(index).line + 1
          if (nextLine >= editor.lineCount()) break
          index = editor.getLineStartOffset(nextLine)
        }
      }
    }
    // entire editor was searched, but no match found
    return VimMatchResult.Failure(VimRegexErrors.E486)
  }

  /**
   * Returns the first match of a pattern in the editor, that comes before the startIndex
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   *
   * @return The first match found in the editor before startIndex
   */
  public fun findPrevious(
    editor: VimEditor,
    startIndex: Int = 0,
    options: List<VimRegexOptions> = emptyList()
  ): VimMatchResult {
    val startLine = editor.offsetToBufferPosition(startIndex).line
    val result = findLastMatchInLine(editor, startLine, startIndex - 1)
    if (result is VimMatchResult.Success && result.range.startOffset < startIndex) {
        // there is a match at this line that starts before the startIndex
        return result
    } else {
      // try searching in previous lines, line by line, and if necessary wrap around to the last line
      var currentLine = startLine - 1
      var wrappedAround = false
      while (!(wrappedAround && currentLine < startLine)) {
        if (currentLine < 0) {
          currentLine = editor.lineCount() - 1
          wrappedAround = true
        } else {
          val previous = findLastMatchInLine(editor, currentLine)
          if (previous is VimMatchResult.Success) return previous
          else currentLine--
        }
      }
      // there are no matches in the entire file
      return VimMatchResult.Failure(VimRegexErrors.E486)
    }
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
    options: List<VimRegexOptions> = emptyList()
  ): VimMatchResult {
    var index = editor.getLineStartOffset(line)
    var prevResult: VimMatchResult = VimMatchResult.Failure(VimRegexErrors.E486)
    while (index <= maxIndex) {
      val result = simulateNonExactNFA(editor, index, options)
      when (result) {
        // no more matches in this line, break out of the loop
        is VimMatchResult.Failure -> break
        is VimMatchResult.Success -> {
          // no more relevant matches in this line, break out of the loop
          if (result.range.startOffset > maxIndex) break

          // match found, try to find more after it
          prevResult = result
          index = if (result.range.startOffset == result.range.endOffset) result.range.endOffset + 1 else result.range.endOffset
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
  public fun findAll(
    editor: VimEditor,
    startIndex: Int = 0,
    maxIndex: Int = editor.text().length,
    options: List<VimRegexOptions> = emptyList()
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

  /**
   * Attempts to match a pattern exactly at the specified
   * index in the editor text.
   *
   * @param editor The editor where to look for the match in
   * @param index  The index to start the match
   *
   * @return The match, either successful or not, found at the specified index
   */
  public fun matchAt(
    editor: VimEditor,
    index: Int,
    options: List<VimRegexOptions> = emptyList()
  ): VimMatchResult {
    return simulateNFA(editor, index, options)
  }

  /**
   * Attempts to match the entire editor against the pattern.
   *
   * @param editor The editor where to look for the match in
   *
   * @return The match, either successful or not, when matching against the entire editor
   */
  public fun matchEntire(
    editor: VimEditor,
    options: List<VimRegexOptions> = emptyList()
  ): VimMatchResult {
    val result = simulateNFA(editor, options=options)
    return when (result) {
      is VimMatchResult.Failure -> result
      is VimMatchResult.Success -> {
        if (result.range.endOffset == editor.text().length) result
        else VimMatchResult.Failure(VimRegexErrors.E486) // create a more appropriate error code?
      }
    }
  }

  /**
   * Indicates whether the pattern matches the entire editor.
   *
   * @param editor The editor where to look for the match in
   *
   * @return True if the entire editor matches, false otherwise
   */
  public fun matches(
    editor: VimEditor,
    options: List<VimRegexOptions> = emptyList()
  ): Boolean {
    val result = simulateNFA(editor, options=options)
    return when (result) {
      is VimMatchResult.Failure -> false
      is VimMatchResult.Success -> result.range.endOffset == editor.text().length
    }
  }

  /**
   * Checks if a pattern matches a part of the editor
   * starting exactly at the specified index.
   *
   * @param editor The editor where to look for the match in
   *
   * @return True if there is a successful match starting at the specified index, false otherwise
   */
  public fun matchesAt(
    editor: VimEditor,
    index: Int,
    options: List<VimRegexOptions> = emptyList()
  ): Boolean {
    return when (simulateNFA(editor, index, options)) {
      is VimMatchResult.Success -> true
      is VimMatchResult.Failure -> false
    }
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
  private fun simulateNFA(editor: VimEditor, index: Int = 0, options: List<VimRegexOptions>): VimMatchResult {
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
  private fun simulateNonExactNFA(editor: VimEditor, index: Int = 0, options: List<VimRegexOptions>): VimMatchResult {
    return VimRegexEngine.simulate(nonExactNFA, editor, index, shouldIgnoreCase(options))
  }

  /**
   * Determines, based on information that comes from the parser and other
   * options that may be set, whether to ignore case.
   */
  private fun shouldIgnoreCase(options: List<VimRegexOptions>): Boolean {
    return when (caseSensitivitySettings) {
      CaseSensitivitySettings.NO_IGNORE_CASE -> false
      CaseSensitivitySettings.IGNORE_CASE -> true
      CaseSensitivitySettings.DEFAULT -> {
        if (options.contains(VimRegexOptions.IGNORE_CASE)) true
        else if (options.contains(VimRegexOptions.SMART_CASE)) true // TODO
        else false
      }
    }
  }
}