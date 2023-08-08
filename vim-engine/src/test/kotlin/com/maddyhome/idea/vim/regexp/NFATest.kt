/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.parser.RegexParser
import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
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

  @Test
  fun `text sequence of any characters`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      ".*",
      0 until 11
    )
  }

  @Test
  fun `test sequence of any characters with newline`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\_.*",
      0 until 128
    )
  }

  @Test
  fun `test single cursor`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lo\\%#rem",
      0 until 5,
      carets = listOf(2)
    )
  }

  @Test
  fun `test single cursor should fail`() {
    assertFailure(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\%#Lorem",
      carets = listOf(2)
    )
  }

  @Test
  fun `test words separated by spaces`() {
    assertCorrectRange(
      "Lorem   \t   Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\v\\w+\\s+\\w+",
      0 until 17
    )
  }

  @Test
  fun `test date format 1`() {
    assertCorrectRange(
      "08-08-2023",
      "\\v\\d{2}%(-|/)\\d{2}%(-|/)%(\\d{4}|\\d{2})",
      0 until 10
    )
  }

  @Test
  fun `test date format 2`() {
    assertCorrectRange(
      "08/08/2023",
      "\\v\\d{2}%(-|/)\\d{2}%(-|/)%(\\d{4}|\\d{2})",
      0 until 10
    )
  }

  @Test
  fun `test date format 3`() {
    assertCorrectRange(
      "08/08/23",
      "\\v\\d{2}%(-|/)\\d{2}%(-|/)%(\\d{4}|\\d{2})",
      0 until 8
    )
  }

  @Test
  fun `test hexadecimal number 1`() {
    assertCorrectRange(
      "0x193ab3f is a hexadecimal number",
      "\\v%(0x)?\\x+",
      0 until 9
    )
  }

  @Test
  fun `test hexadecimal number 2`() {
    assertCorrectRange(
      "abcdef23901a is also a hexadecimal number",
      "\\v%(0x)?\\x+",
      0 until 12
    )
  }

  @Test
  fun `test name surname`() {
    assertCorrectRange(
      "Emanuel Gestosa",
      "\\v\\u\\l+\\s+\\u\\l+",
      0 until 15
    )
  }

  @Test
  fun `test name surname invalid`() {
    assertFailure(
      "EmaNuel Gestosa",
      "\\v\\u\\l+\\s+\\u\\l+"
    )
  }

  @Test
  fun `test sequence of digits`() {
    assertCorrectRange(
      "45135abc235",
      "\\d\\+",
      0 until 5
    )
  }

  @Test
  fun `test sequence of not digits`() {
    assertCorrectRange(
      "abcd123efg",
      "\\D\\+",
      0 until 4
    )
  }

  @Test
  fun `test empty collection`() {
    assertCorrectRange(
      "[]abc",
      "[]",
      0 until 2
    )
  }

  @Test
  fun `test empty negated collection`() {
    assertCorrectRange(
      "[^]abc",
      "[^]",
      0 until 3
    )
  }

  @Test
  fun `test collection a to z and 0`() {
    assertCorrectRange(
      "abcd0efg1hij",
      "[a-z0]\\+",
      0 until 8
    )
  }

  @Test
  fun `test collection a to z and 0 negated`() {
    assertCorrectRange(
      "ABCD0EFG1HIJ",
      "[^a-z0]\\+",
      0 until 4
    )
  }

  @Test
  fun `test collection dash and a to z`() {
    assertCorrectRange(
      "a-b-c-d_f-g",
      "[-a-z]\\+",
      0 until 7
    )
  }

  @Test
  fun `test collection a, dash and z`() {
    assertCorrectRange(
      "az-e",
      "[a\\-z]\\+",
      0 until 3
    )
  }

  private fun assertCorrectRange(
    text: CharSequence,
    pattern: String,
    expectedResultRange:
    IntRange,
    offset: Int = 0,
    carets: List<Int> = emptyList()
  ) {
    val editor = buildEditor(text, carets)
    val nfa = buildNFA(pattern)
    val result = nfa.simulate(editor, offset)
    when (result) {
      is VimMatchResult.Failure -> fail("Expected to find match")
      is VimMatchResult.Success -> assertEquals(expectedResultRange, result.range)
    }
  }

  private fun assertCorrectGroupRange(
    text: CharSequence,
    pattern: String,
    expectedResultRange: IntRange,
    groupNumber: Int,
    offset: Int = 0,
    carets: List<Int> = emptyList()
  ) {
    val editor = buildEditor(text, carets)
    val nfa = buildNFA(pattern)
    val result = nfa.simulate(editor, offset)
    when (result) {
      is VimMatchResult.Failure -> fail("Expected to find match")
      is VimMatchResult.Success -> assertEquals(expectedResultRange, result.groups.get(groupNumber)?.range)
    }
  }

  private fun assertFailure(text: CharSequence, pattern: String, offset: Int = 0, carets: List<Int> = emptyList()) {
    val editor = buildEditor(text, carets)
    val nfa = buildNFA(pattern)
    assertTrue(nfa.simulate(editor, offset) is VimMatchResult.Failure)
  }

  private fun buildEditor(text: CharSequence, carets: List<Int> = emptyList()) : VimEditor {
    val editorMock = mock<VimEditor>()
    whenever(editorMock.text()).thenReturn(text)

    val trueCarets = ArrayList<VimCaret>()
    for (caret in carets) {
      val caretMock = mock<VimCaret>()
      whenever(caretMock.offset).thenReturn(Offset(caret))
      trueCarets.add(caretMock)
    }
    whenever(editorMock.carets()).thenReturn(trueCarets)
    return editorMock
  }

  private fun buildNFA(pattern: String) : NFA {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    val tree = parser.pattern()
    return PatternVisitor().visit(tree)
  }
}