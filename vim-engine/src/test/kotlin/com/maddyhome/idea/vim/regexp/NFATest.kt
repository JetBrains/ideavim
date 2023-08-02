/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.RegexParser
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NFATest {

  /**
   * Test simulation of pattern "a\|b"
   */
  @Test
  fun `test simulate 'a or b' succeeds`() {
    val nfa = buildNFA("a\\|b")
    assertTrue(nfa.simulate("a"))
    assertTrue(nfa.simulate("b"))
  }

  /**
   * Test simulation of pattern "a\|b"
   */
  @Test
  fun `test simulate 'a or b' fails`() {
    val nfa = buildNFA("a\\|b")
    assertFalse(nfa.simulate("c"))
  }

  private fun generateTree(pattern: String): ParseTree {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    return parser.pattern()
  }

  private fun buildNFA(pattern: String) : NFA {
    val tree = generateTree(pattern)
    val patternVisitor = PatternVisitor()
    return patternVisitor.visit(tree)
  }
}