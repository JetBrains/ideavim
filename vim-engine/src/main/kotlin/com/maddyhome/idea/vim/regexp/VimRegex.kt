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

public class VimRegex(pattern: String) {

  private val nfa: NFA

  init {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    val tree = parser.pattern()
    val patternVisitor = PatternVisitor()
    this.nfa = patternVisitor.visit(tree)
  }

  public fun containsMatchIn(editor: VimEditor) : Boolean {
    return nfa.simulate(editor) is VimMatchResult.Success
  }
}