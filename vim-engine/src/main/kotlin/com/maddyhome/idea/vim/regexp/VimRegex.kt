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
import com.maddyhome.idea.vim.regexp.parser.RegexParser
import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * Represents a compiled Vim regular expression. Provides methods to
 * match, replace and split strings in the editor with a pattern.
 * To learn about Vim's pattern syntax see :help pattern
 */
public class VimRegex(pattern: String) {

  /**
   * The NFA representing the compiled regular expression
   */
  private val nfa: NFA

  init {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    val tree = parser.pattern()
    val patternVisitor = PatternVisitor()
    this.nfa = patternVisitor.visit(tree)
  }

  /**
   * Indicates whether the regular expression can find at least one match in the specified editor
   *
   * @param editor The editor where to look for the match in
   *
   * @return True if any match was found, false otherwise
   */
  public fun containsMatchIn(editor: VimEditor): Boolean {
    var startIndex = 0
    while (startIndex <= editor.text().length) {
      val result = nfa.simulate(editor, startIndex)
      when (result) {
        /**
         * A match was found
         */
        is VimMatchResult.Success -> return true

        /**
         * No match found yet, try searching on next index
         */
        is VimMatchResult.Failure -> startIndex++
      }
    }

    /**
     * Entire editor was searched, but no match found
     */
    return false
  }

  /**
   * Returns the first match of a regular expression in the editor, beginning at the specified index.
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   */
  public fun find(
    editor: VimEditor,
    startIndex: Int = 0
  ): VimMatchResult {
    var index = startIndex
    while (index <= editor.text().length) {
      val result = nfa.simulate(editor, index)
      when (result) {
        /**
         * A match was found
         */
        is VimMatchResult.Success -> return result

        /**
         * No match found yet, try searching on next index
         */
        is VimMatchResult.Failure -> index++
      }
    }

    /**
     * Entire editor was searched, but no match found
     */
    return VimMatchResult.Failure
  }

  /**
   * Returns a sequence of all occurrences of a regular expression within
   * the editor, beginning at the specified index
   *
   * @param editor     The editor where to look for the match in
   * @param startIndex The index to start the find
   */
  public fun findAll(
    editor: VimEditor,
    startIndex: Int = 0
  ): Sequence<VimMatchResult.Success> {
    var index = startIndex
    val foundMatches: MutableList<VimMatchResult.Success> = emptyList<VimMatchResult.Success>().toMutableList()
    while (index <= editor.text().length) {
      val result = nfa.simulate(editor, index)
      when (result) {
        /**
         * A match was found, add it to foundMatches and increment
         * next index accordingly
         */
        is VimMatchResult.Success -> {
          foundMatches.add(result)
          if (result.range.isEmpty()) index++
          else index = result.range.last + 1
        }

        /**
         * No match found starting on this index, try searching on next index
         */
        is VimMatchResult.Failure -> index++
      }
    }
    return foundMatches.asSequence()
  }

  /**
   * Attempts to match a regular expression exactly at the specified
   * index in the editor text.
   *
   * @param editor The editor where to look for the match in
   * @param index  The index to start the match
   */
  public fun matchAt(
    editor: VimEditor,
    index: Int
  ): VimMatchResult {
    return nfa.simulate(editor, index)
  }

  /**
   * Attempts to match the entire editor against the pattern.
   *
   * @param editor The editor where to look for the match in
   */
  public fun matchEntire(
    editor: VimEditor
  ): VimMatchResult {
    val result = nfa.simulate(editor)
    return when (result) {
      is VimMatchResult.Failure -> result
      is VimMatchResult.Success -> {
        if (result.range.last + 1 == editor.text().length) result
        else VimMatchResult.Failure
      }
    }
  }

  /**
   * Indicates whether the regular expression matches the entire editor.
   *
   * @param editor The editor where to look for the match in
   */
  public fun matches(
    editor: VimEditor
  ): Boolean {
    val result = nfa.simulate(editor)
    return when (result) {
      is VimMatchResult.Failure -> false
      is VimMatchResult.Success -> result.range.last + 1 == editor.text().length
    }
  }
}