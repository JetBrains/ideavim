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
  fun `test lazy multi doesn't consume anything`() {
    assertCorrectRange(
      "aaaaa",
      "a\\{-}",
      IntRange.EMPTY
    )
  }

  @Test
  fun `test closest matching quotes`() {
    assertCorrectRange(
      "\"Lorem\" \"Ipsum\"",
      "\".\\{-}\"",
      0 until 7
    )
  }

  @Test
  fun `test farthest matching quotes`() {
    assertCorrectRange(
      "\"Lorem\" \"Ipsum\"",
      "\".\\{}\"",
      0 until 15
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

  @Test
  fun `test collection backslash and a`() {
    assertCorrectRange(
      "\\aa\\bc",
      "[\\a]\\+",
      0 until 4
    )
  }

  @Test
  fun `test collection unicode a to unicode z`() {
    assertCorrectRange(
      "abcdf123",
      "[\\u61-\\u007a]\\+",
      0 until 5
    )
  }

  @Test
  fun `test collection backslash, u and z`() {
    assertCorrectRange(
      "uz\\zuabc",
      "[\\uz]\\+",
      0 until 5
    )
  }

  @Test
  fun `test set start of match`() {
    assertCorrectRange(
      "endif",
      "end\\zsif",
      3 until 5
    )
  }

  @Test
  fun `test set end of match`() {
    assertCorrectRange(
      "endif",
      "end\\zeif",
      0 until 3
    )
  }

  @Test
  fun `test set multiple start of match`() {
    assertCorrectRange(
      "endif",
      "\\zse\\zsn\\zsd\\zsif",
      3 until 5
    )
  }

  @Test
  fun `test set multiple end of match`() {
    assertCorrectRange(
      "endif",
      "\\zee\\zen\\zed\\zeif",
      0 until 3
    )
  }

  @Test
  fun `test set match start after set match end`() {
    assertCorrectRange(
      "endif",
      "\\zeend\\zsif",
      3 until 5
    )
  }

  @Test
  fun `test backreference to group 1`() {
    assertCorrectRange(
      "cat cat",
      "\\v(dog|cat) \\1",
      0 until 7
    )
  }

  @Test
  fun `test backreference should fail`() {
    assertFailure(
      "dog cat",
      "\\v(dog|cat) \\1"
    )
  }

  @Test
  fun `test backreference to uncaptured group`() {
    assertCorrectRange(
      "aaa",
      "\\v(b)*\\1",
      IntRange.EMPTY
    )
  }

  @Test
  fun `test back-referenced group value updates`() {
    assertCorrectRange(
      "aaabb",
      "\\v(a|b){1,100}\\1",
      0 until 5
    )
  }

  @Test
  fun `test capturing inner nested group`() {
    assertCorrectGroupRange(
      "abaabb",
      "\\v(a(b)?)+",
      4 until 5,
      2
    )
  }

  @Test
  fun `test case insensitive word`() {
    assertCorrectRange(
      "IdeaVim",
      "ideavim",
      0 until 7,
      ignoreCase = true
    )
  }

  @Test
  fun `test case insensitive collection`() {
    assertCorrectRange(
      "IdeaVim",
      "[a-z]\\+",
      0 until 7,
      ignoreCase = true
    )
  }

  @Test
  fun `test character classes never ignore case`() {
    assertFailure(
      "IdeaVim",
      "\\l\\+",
      ignoreCase = true
    )
  }

  @Test
  fun `test start of file`() {
    assertCorrectRange(
      "IdeaVim",
      "\\%^Idea",
      0 until 4
    )
  }
  @Test
  fun `test start of file should fail`() {
    assertFailure(
      "IdeaVim",
      "\\%^Vim",
      4
    )
  }

  @Test
  fun `test end of file`()  {
    assertCorrectRange(
      "IdeaVim",
      "Vim\\%$",
      4 until 7,
      4
    )
  }

  @Test
  fun `test end of file should fail`() {
    assertFailure(
      "IdeaVim",
      "Idea\\%$"
    )
  }

  @Test
  fun `test start and end of file`() {
    assertCorrectRange(
      "IdeaVim",
      "\\%^IdeaVim\\%$",
      0 until 7
    )
  }

  @Test
  fun `test for empty file`() {
    assertCorrectRange(
      "",
      "\\v%^%$",
      IntRange.EMPTY
    )
  }

  @Test
  fun `test for empty file should fail`() {
    assertFailure(
      "IdeaVim",
      "\\v%^%$"
    )
  }

  @Test
  fun `test start of line`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "^Lorem",
      13 until 18,
      13
    )
  }

  @Test
  fun `test start of line should fail`() {
    assertFailure(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "^Ipsum",
      6
    )
  }

  @Test
  fun `test end of line`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Ipsum$",
      6 until 11,
      6
    )
  }

  @Test
  fun `test end of line should fail`() {
    assertFailure(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem$"
    )
  }

  @Test
  fun `test start of line after alternation`() {
    assertCorrectRange(
      "dog barks",
      "^cat\\|^dog",
      0 until 3
    )
  }

  @Test
  fun `test end of line before alternation`() {
    assertCorrectRange(
      "cat\n" +
      "meows",
      "cat$\\|dog$",
      0 until 3
    )
  }

  @Test
  fun `test start and end of line inside parenthesis`() {
    assertCorrectRange(
      "cat meows",
      "\\v(^(cat|dog)) ((meows|barks)$)",
      0 until 9
    )
  }

  @Test
  fun `test caret is taken literally`() {
    assertCorrectRange(
      "the symbol ^ is known as caret.",
      "^.\\+^.\\+$",
      0 until 31
    )
  }

  @Test
  fun `test dollar sign is taken literally`() {
    assertCorrectRange(
      "the symbol for the dollar is $.",
      "^.\\+$.\\+$",
      0 until 31
    )
  }

  @Test
  fun `test caret is taken literally at the start of pattern`() {
    assertCorrectRange(
      "^ is known as caret.",
      "\\^ is known",
      0 until 10
    )
  }

  @Test
  fun `test dollar sign is taken literally at the end of pattern`() {
    assertCorrectRange(
      "the symbol for the dollar is $.",
      "dollar is \\$",
      19 until 30,
      19
    )
  }

  @Test
  fun `test start of line anywhere in pattern`() {
    assertCorrectRange(
      "Lorem Ipsum Lorem\n" + // added an extra 'Lorem' that isn't at start of line
        "\n" +
        "Lorem ipsum dolor sit amet,\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "\\_.\\+\\_^Lorem",
      0 until 24
    )
  }

  @Test
  fun `test end of line anywhere in pattern`() {
    assertCorrectRange(
      "Lorem Ipsum\n" +
        "\n" +
        "Lorem ipsum dolor sit amet, Lorem\n" +
        "consectetur adipiscing elit\n" +
        "Sed in orci mauris.\n" +
        "Cras id tellus in ex imperdiet egestas.",
      "Lorem Ipsum\\_$\\_s*",
      0 until 13
    )
  }

  @Test
  fun `test atomic group`() {
    assertCorrectRange(
      "aaab",
      "\\(a*\\)\\@>b",
      0 until 4

    )
  }

  @Test
  fun `test atomic group should fail`() {
    assertFailure(
      "aaa",
      "\\(a*\\)\\@>a"
    )
  }

  private fun assertCorrectRange(
    text: CharSequence,
    pattern: String,
    expectedResultRange:
    IntRange,
    offset: Int = 0,
    carets: List<Int> = emptyList(),
    ignoreCase: Boolean = false
  ) {
    val editor = buildEditor(text, carets)
    val nfa = buildNFA(pattern)
    val result = nfa.simulate(editor, offset, isCaseInsensitive = ignoreCase)
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

  private fun assertFailure(text: CharSequence, pattern: String, offset: Int = 0, carets: List<Int> = emptyList(), ignoreCase: Boolean = false) {
    val editor = buildEditor(text, carets)
    val nfa = buildNFA(pattern)
    assertTrue(nfa.simulate(editor, offset, ignoreCase) is VimMatchResult.Failure)
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