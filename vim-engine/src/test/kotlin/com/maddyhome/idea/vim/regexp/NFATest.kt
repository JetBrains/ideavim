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
import com.maddyhome.idea.vim.api.VimScrollingModel
import com.maddyhome.idea.vim.api.VimSelectionModel
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.VirtualFile
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.parser.RegexParser
import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class NFATest {
  @Test
  fun `test match not found`() {
    assertFailure(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "VIM"
    )
  }

  @Test
  fun `test concatenation from start`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
      "\n" +
      "Lorem ipsum dolor sit amet,\n" +
      "consectetur adipiscing elit\n" +
      "Sed in orci mauris.\n" +
      "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      0 until 5
    )
  }

  @Test
  fun `test concatenation from offset`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem",
      13 until 18,
      13
    )
  }

  @Test
  fun `test concatenation with escaped char`() {
    assertCorrectRange(
      "a*bcd",
      "a\\*",
      0 until 2,
    )
  }

  @Test
  fun `test star multi`() {
    assertCorrectRange(
      "aaaaabcd",
      "a*",
      0 until 5,
    )
  }

  @Test
  fun `test star multi empty match`() {
    assertCorrectRange(
      "bcd",
      "a*",
      IntRange.EMPTY
    )
  }

  @Test
  fun `test plus multi`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\+",
      0 until 5,
    )
  }

  @Test
  fun `test plus multi should fail`() {
    assertFailure(
      "bcd",
      "a\\+"
    )
  }

  @Test
  fun `test range multi both bounds`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\{0,3}",
      0 until 3,
    )
  }

  @Test
  fun `test range multi lower bound`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\{2,}",
      0 until 5,
    )
  }

  @Test
  fun `test range multi upper bound`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\{,2}",
      0 until 2,
    )
  }

  @Test
  fun `test range unbounded`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\{}",
      0 until 5,
    )
  }

  @Test
  fun `test range unbounded with comma`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\{,}",
      0 until 5,
    )
  }

  @Test
  fun `test range absolute bound`() {
    assertCorrectRange(
      "aaaaabcd",
      "a\\{2}",
      0 until 2,
    )
  }

  @Test
  fun `test range should fail`() {
    assertFailure(
      "aaaaabcd",
      "a\\{6,}"
    )
  }

  @Test
  fun `test group`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\v(Lorem)",
      0 until 5
    )
  }

  @Test
  fun `test group followed by word`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\v(Lorem) Ipsum",
      0 until 11
    )
  }

  @Test
  fun `test capture group 1`() {
    assertCorrectGroupRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\v(Lorem) Ipsum",
      0 until 5,
      1
    )
  }

  @Test
  fun `test capture group 2`() {
    assertCorrectGroupRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\v(Lorem) (Ipsum)",
      6 until 11,
      2
    )
  }

  @Test
  fun `test group updates range`() {
    assertCorrectGroupRange(
      "abababc",
      "\\v(ab)*c",
      4 until 6,
      1
    )
  }

  @Test
  fun `test empty group`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\v()",
        IntRange.EMPTY
    )
  }

  @Test
  fun `test alternation with star multi`() {
    assertCorrectRange(
      "abc",
      "\\v%(a|b)*c",
      0 until 3
    )
  }

  @Test
  fun `test star multi has to backtrack`() {
    assertCorrectRange(
      "a",
      "a*a",
      0 until 1
    )
  }

  @Test
  fun `test multiple paths to loop`() {
    assertCorrectRange(
      "ababc",
      "\\v(a|b)+c=",
      0 until 5
    )
  }

  @Test
  fun `test nested multi`() {
    assertCorrectRange(
      "aaaa",
      "\\v(a=)*",
      0 until 4
    )
  }

  @Test
  fun `test nested multi madness`() {
    assertCorrectRange(
      "acabcdabcacd",
      "\\v((ab=c+)+d)*",
      0 until 12
    )
  }

  private fun assertCorrectRange(text: CharSequence, pattern: String, expectedResultRange: IntRange, offset: Int = 0) {
    val editor = buildEditor(text)
    val nfa = buildNFA(pattern)
    val result = nfa.simulate(editor, offset)
    when (result) {
      is VimMatchResult.Failure -> fail("Expected to find match")
      is VimMatchResult.Success -> assertEquals(expectedResultRange, result.range)
    }
  }

  private fun assertCorrectGroupRange(text: CharSequence, pattern: String, expectedResultRange: IntRange, groupNumber: Int, offset: Int = 0) {
    val editor = buildEditor(text)
    val nfa = buildNFA(pattern)
    val result = nfa.simulate(editor, offset)
    when (result) {
      is VimMatchResult.Failure -> fail("Expected to find match")
      is VimMatchResult.Success -> assertEquals(expectedResultRange, result.groups.get(groupNumber)?.range)
    }
  }

  private fun assertFailure(text: CharSequence, pattern: String, offset: Int = 0) {
    val editor = buildEditor(text)
    val nfa = buildNFA(pattern)
    assertTrue(nfa.simulate(editor, offset) is VimMatchResult.Failure)
  }

  private fun buildEditor(text: CharSequence) : VimEditor {
    return VimEditorMock(text)
  }

  private fun buildNFA(pattern: String) : NFA {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    val tree = parser.pattern()
    return PatternVisitor().visit(tree)
  }
}