/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.nfa.matcher.DotMatcher
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
   * TODO: in my option only the find() and findAll() methods are necessary.
   *
   * The replace methods (not present yet) should probably be implemented
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
  private val nfaExact: NFA

  /**
   * The NFA representing the compiled pattern, preceded by any characters.
   * Equivalent to /\_.\{-}pattern
   */
  private val nfaNonExact: NFA

  /**
   * The NFA representing the compiled pattern, preceded by any characters except line breaks.
   * Equivalent to /.\{-}pattern
   */
  private val nfaNonExactSingleLine: NFA

  init {
    val parseResult = VimRegexParser.parse(pattern)

    when (parseResult) {
      is VimRegexParserResult.Failure -> throw RuntimeException(parseResult.errorCode.toString()) // TODO: use different exception
      is VimRegexParserResult.Success -> {
        nfaExact = PatternVisitor.visit(parseResult.tree)
        nfaNonExact = NFA.fromMatcher(DotMatcher(true)).closure(false).concatenate(nfaExact)
        nfaNonExactSingleLine = NFA.fromMatcher(DotMatcher(false)).closure(false).concatenate(nfaExact)
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
  public fun containsMatchIn(editor: VimEditor): Boolean {
    return simulateNFANonExact(editor) is VimMatchResult.Success
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
    startIndex: Int = 0
  ): VimMatchResult {
    val lineStartIndex = editor.getLineStartOffset(editor.offsetToBufferPosition(startIndex).line)
    var result = simulateNFANonExactSingleLine(editor, lineStartIndex)
    while (true)
      when (result) {
        is VimMatchResult.Success -> {
          // the match comes after the startIndex, return it
          if (result.range.startOffset > startIndex) return result
          // there is a match but starts before the startIndex, try again starting from the end of this match
          else result = simulateNFANonExactSingleLine(editor, if (result.range.startOffset == result.range.endOffset) result.range.endOffset + 1 else result.range.endOffset)
        }
        is VimMatchResult.Failure -> {
          // there is no match that starts in the line of startIndex, find a match that starts anywhere after startIndex
          val nextMatch = simulateNFANonExact(editor, startIndex)

          // match found, return it
          return if (nextMatch is VimMatchResult.Success) nextMatch
          // no match found, try from the start of the editor
          else if (startIndex != 0) simulateNFANonExact(editor, 0)
          // there are no matches in the entire file
          else VimMatchResult.Failure(VimRegexErrors.E486)
        }
      }
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
    startIndex: Int = 0
  ): VimMatchResult {
    val startLine = editor.offsetToBufferPosition(startIndex).line
    val result = findLastMatchInLine(editor, startLine, startIndex)
    if (result is VimMatchResult.Success && result.range.startOffset < startIndex) {
        // there is a match at this line that starts before the startIndex
        return result
    } else {
      // try searching in previous lines, line by line, and if necessary wrap around to the last line
      var currentLine = startLine - 1
      var wrappedAround = false
      while (!(wrappedAround && currentLine <= startLine)) {
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
    maxIndex: Int = editor.getLineEndOffset(line)
  ): VimMatchResult {
    var index = editor.getLineStartOffset(line)
    var prevResult: VimMatchResult = VimMatchResult.Failure(VimRegexErrors.E486)
    while (index < maxIndex) {
      val result = simulateNFANonExactSingleLine(editor, index)
      when (result) {
        // no more matches in this line, break out of the loop
        is VimMatchResult.Failure -> break
        is VimMatchResult.Success -> {
          // no more matches in this line, break out of the loop
          if (result.range.startOffset > editor.getLineEndOffset(line)) break


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
    startIndex: Int = 0
  ): Sequence<VimMatchResult.Success> {
    var index = startIndex
    val foundMatches: MutableList<VimMatchResult.Success> = emptyList<VimMatchResult.Success>().toMutableList()
    while (index <= editor.text().length) {
      val result = simulateNFANonExact(editor, index)
      when (result) {
        /**
         * A match was found, add it to foundMatches and increment
         * next index accordingly
         */
        is VimMatchResult.Success -> {
          foundMatches.add(result)
          if (result.range.startOffset == result.range.endOffset) index++
          else index = result.range.endOffset
        }

        /**
         * No more matches found. Return.
         */
        is VimMatchResult.Failure -> return foundMatches.asSequence()
      }
    }
    return foundMatches.asSequence()
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
    index: Int
  ): VimMatchResult {
    return simulateNFAExact(editor, index)
  }

  /**
   * Attempts to match the entire editor against the pattern.
   *
   * @param editor The editor where to look for the match in
   *
   * @return The match, either successful or not, when matching against the entire editor
   */
  public fun matchEntire(
    editor: VimEditor
  ): VimMatchResult {
    val result = simulateNFAExact(editor)
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
    editor: VimEditor
  ): Boolean {
    val result = simulateNFAExact(editor)
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
    index: Int
  ): Boolean {
    return when (simulateNFAExact(editor, index)) {
      is VimMatchResult.Success -> true
      is VimMatchResult.Failure -> false
    }
  }

  /**
   * Simulates the internal exact NFA with the determined flags,
   * started on a given index.
   *
   * @param editor The editor that is used for the simulation
   * @param index  The index where the simulation should start
   *
   * @return The resulting match result
   */
  private fun simulateNFAExact(editor: VimEditor, index: Int = 0) : VimMatchResult {
    return nfaExact.simulate(editor, index, shouldIgnoreCase())
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
  private fun simulateNFANonExact(editor: VimEditor, index: Int = 0) : VimMatchResult {
    return nfaNonExact.simulate(editor, index, shouldIgnoreCase())
  }

  /**
   * Simulates the internal non-exact single line NFA with the determined flags,
   * started on a given index.
   *
   * @param editor The editor that is used for the simulation
   * @param index  The index where the simulation should start
   *
   * @return The resulting match result
   */
  private fun simulateNFANonExactSingleLine(editor: VimEditor, index: Int = 0) : VimMatchResult {
    return nfaNonExactSingleLine.simulate(editor, index, shouldIgnoreCase())
  }

  /**
   * Determines, based on information that comes from the parser and other
   * options that may be set, whether to ignore case.
   */
  private fun shouldIgnoreCase() : Boolean {
    return when (caseSensitivitySettings) {
      CaseSensitivitySettings.NO_IGNORE_CASE -> false
      CaseSensitivitySettings.IGNORE_CASE -> true
      CaseSensitivitySettings.DEFAULT -> false // TODO: check if ignorecase or smartcase is set
    }
  }
}