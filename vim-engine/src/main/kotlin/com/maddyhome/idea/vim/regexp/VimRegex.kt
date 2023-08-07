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
  public fun containsMatchIn(editor: VimEditor) : Boolean {
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
}