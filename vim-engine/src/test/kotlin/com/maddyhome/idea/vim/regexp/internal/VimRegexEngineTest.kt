/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.internal

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.CARET
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.END
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.MARK
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.START
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.getMatchRanges
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.mockEditor
import com.maddyhome.idea.vim.regexp.VimRegexTestUtils.mockEditorFromText
import com.maddyhome.idea.vim.regexp.engine.VimRegexEngine
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.DotMatcher
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class VimRegexEngineTest {
  @Test
  fun `test match not found`() {
    assertFailure(
      """
      	|Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "VIM"
    )
  }

  @Test
  fun `test concatenation from start`() {
    doTest(
      """
        |${START}Lorem$END Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "Lorem",
    )
  }

  @Test
  fun `test concatenation from offset`() {
    doTest(
      """
        |Lorem Ipsum
        |
        |${START}Lorem$END ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "Lorem",
      13
    )
  }

  @Test
  fun `test concatenation with escaped char`() {
    doTest(
      "${START}a*${END}bcd",
      "a\\*",
    )
  }

  @Test
  fun `test star multi`() {
    doTest(
      "${START}aaaaa${END}bcd",
      "a*",
    )
  }

  @Test
  fun `test star multi empty match`() {
    doTest(
      "$START${END}bcd",
      "a*",
    )
  }

  @Test
  fun `test plus multi`() {
    doTest(
      "${START}aaaaa${END}bcd",
      "a\\+",
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
    doTest(
      "${START}aaa${END}aabcd",
      "a\\{0,3}",
    )
  }

  @Test
  fun `test range multi lower bound`() {
    doTest(
      "${START}aaaaa${END}bcd",
      "a\\{2,}",
    )
  }

  @Test
  fun `test range multi upper bound`() {
    doTest(
      "${START}aa${END}aaabcd",
      "a\\{,2}",
    )
  }

  @Test
  fun `test range unbounded`() {
    doTest(
      "${START}aaaaa${END}bcd",
      "a\\{}",
    )
  }

  @Test
  fun `test range unbounded with comma`() {
    doTest(
      "${START}aaaaa${END}bcd",
      "a\\{,}",
    )
  }

  @Test
  fun `test range absolute bound`() {
    doTest(
      "${START}aa${END}aaabcd",
      "a\\{2}",
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
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v(Lorem)",
    )
  }

  @Test
  fun `test group followed by word`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "\\v(Lorem) Ipsum",
    )
  }

  @Test
  fun `test capture group 1`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v(Lorem) Ipsum",
      groupNumber = 1
    )
  }

  @Test
  fun `test capture group 2`() {
    doTest(
      "Lorem ${START}Ipsum$END",
      "\\v(Lorem) (Ipsum)",
      groupNumber = 2
    )
  }

  @Test
  fun `test group updates range`() {
    doTest(
      "abab${START}ab${END}c",
      "\\v(ab)*c",
      groupNumber = 1
    )
  }

  @Test
  fun `test empty group`() {
    doTest(
      "$START${END}Lorem Ipsum",
      "\\v()",
    )
  }

  @Test
  fun `test alternation with star multi`() {
    doTest(
      "${START}abc$END",
      "\\v%(a|b)*c",
    )
  }

  @Test
  fun `test star multi has to backtrack`() {
    doTest(
      "${START}a$END",
      "a*a",
    )
  }

  @Test
  fun `test multiple paths to loop`() {
    doTest(
      "${START}ababc$END",
      "\\v(a|b)+c=",
    )
  }

  @Test
  fun `test nested multi`() {
    doTest(
      "${START}aaaa$END",
      "\\v(a=)*",
    )
  }

  @Test
  fun `test nested multi madness`() {
    doTest(
      "${START}acabcdabcacd$END",
      "\\v((ab=c+)+d)*",
    )
  }

  @Test
  fun `test lazy multi doesn't consume anything`() {
    doTest(
      "$START${END}aaaaa",
      "a\\{-}",
    )
  }

  @Test
  fun `test closest matching quotes`() {
    doTest(
      """
        |$START"Lorem"$END "Ipsum"
      """.trimMargin(),
      "\".\\{-}\"",
    )
  }

  @Test
  fun `test farthest matching quotes`() {
    doTest(
      """
        |$START"Lorem" "Ipsum"$END
      """.trimMargin(),
      "\".\\{}\"",
    )
  }

  @Test
  fun `text sequence of any characters`() {
    doTest(
      """
        |${START}Lorem Ipsum$END
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      ".*",
    )
  }

  @Test
  fun `test sequence of any characters with newline`() {
    doTest(
      """
        |${START}Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.$END
      """.trimMargin(),
      "\\_.*",
    )
  }

  @Test
  fun `test single cursor`() {
    doTest(
      "${START}Lo${CARET}rem$END Ipsum",
      "Lo\\%#rem",
    )
  }

  @Test
  fun `test single cursor should fail`() {
    assertFailure(
      "Lo${CARET}rem Ipsum",
      "\\%#Lorem"
    )
  }

  @Test
  fun `test words separated by spaces`() {
    doTest(
      "${START}Lorem   \t   Ipsum$END",
      "\\v\\w+\\s+\\w+",
    )
  }

  @Test
  fun `test date format 1`() {
    doTest(
      "${START}08-08-2023$END",
      "\\v\\d{2}%(-|/)\\d{2}%(-|/)%(\\d{4}|\\d{2})",
    )
  }

  @Test
  fun `test date format 2`() {
    doTest(
      "${START}08/08/2023$END",
      "\\v\\d{2}%(-|/)\\d{2}%(-|/)%(\\d{4}|\\d{2})",
    )
  }

  @Test
  fun `test date format 3`() {
    doTest(
      "${START}08/08/23$END",
      "\\v\\d{2}%(-|/)\\d{2}%(-|/)%(\\d{4}|\\d{2})",
    )
  }

  @Test
  fun `test hexadecimal number 1`() {
    doTest(
      "${START}0x193ab3f$END is a hexadecimal number",
      "\\v%(0x)?\\x+",
    )
  }

  @Test
  fun `test hexadecimal number 2`() {
    doTest(
      "${START}abcdef23901a$END is also a hexadecimal number",
      "\\v%(0x)?\\x+",
    )
  }

  @Test
  fun `test name surname`() {
    doTest(
      "${START}Emanuel Gestosa$END",
      "\\v\\u\\l+\\s+\\u\\l+",
    )
  }

  @Test
  fun `test name surname invalid`() {
    assertFailure(
      "EmaNuel gestosa",
      "\\v\\u\\l+\\s+\\u\\l+"
    )
  }

  @Test
  fun `test sequence of digits`() {
    doTest(
      "${START}45135${END}abc235",
      "\\d\\+",
    )
  }

  @Test
  fun `test sequence of not digits`() {
    doTest(
      "${START}abcd${END}123efg",
      "\\D\\+",
    )
  }

  @Test
  fun `test empty collection`() {
    doTest(
      "$START[]${END}abc",
      "[]",
    )
  }

  @Test
  fun `test empty negated collection`() {
    doTest(
      "$START[^]${END}abc",
      "[^]",
    )
  }

  @Test
  fun `test collection a to z and 0`() {
    doTest(
      "${START}abcd0efg${END}1hij",
      "[a-z0]\\+",
    )
  }

  @Test
  fun `test collection a to z and 0 negated`() {
    doTest(
      "${START}ABCD${END}0EFG1HIJ",
      "[^a-z0]\\+",
    )
  }

  @Test
  fun `test collection dash and a to z`() {
    doTest(
      "${START}a-b-c-d${END}_f-g",
      "[-a-z]\\+",
    )
  }

  @Test
  fun `test collection a, dash and z`() {
    doTest(
      "${START}az-${END}e",
      "[a\\-z]\\+",
    )
  }

  @Test
  fun `test collection backslash and a`() {
    doTest(
      "$START\\aa\\${END}bc",
      "[\\a]\\+",
    )
  }

  @Test
  fun `test collection unicode a to unicode z`() {
    doTest(
      "${START}abcdf${END}123",
      "[\\u61-\\u007a]\\+",
    )
  }

  @Test
  fun `test collection backslash, u and z`() {
    doTest(
      "${START}uz\\zu${END}abc",
      "[\\uz]\\+",
    )
  }

  @Test
  fun `test set start of match`() {
    doTest(
      "end${START}if$END",
      "end\\zsif",
    )
  }

  @Test
  fun `test set end of match`() {
    doTest(
      "${START}end${END}if",
      "end\\zeif",
    )
  }

  @Test
  fun `test set multiple start of match`() {
    doTest(
      "end${START}if$END",
      "\\zse\\zsn\\zsd\\zsif",
    )
  }

  @Test
  fun `test set multiple end of match`() {
    doTest(
      "${START}end${END}if",
      "\\zee\\zen\\zed\\zeif",
    )
  }

  @Test
  fun `test set match start after set match end`() {
    doTest(
      "end${START}if$END",
      "\\zeend\\zsif",
    )
  }

  @Test
  fun `test backreference to group 1`() {
    doTest(
      "${START}cat cat$END",
      "\\v(dog|cat) \\1",
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
  fun `test backreference to un-captured group`() {
    doTest(
      "$START${END}aaa",
      "\\v(b)*\\1",
    )
  }

  @Test
  fun `test back-referenced group value updates`() {
    doTest(
      "${START}aaabb$END",
      "\\v(a|b){1,100}\\1",
    )
  }

  @Test
  fun `test capturing inner nested group`() {
    doTest(
      "abaa${START}b${END}b",
      "\\v(a(b)?)+",
      groupNumber = 2
    )
  }

  @Test
  fun `test case insensitive word`() {
    doTest(
      "${START}IdeaVim$END",
      "ideavim",
      ignoreCase = true
    )
  }

  @Test
  fun `test case insensitive collection`() {
    doTest(
      "${START}IdeaVim$END",
      "[a-z]\\+",
      ignoreCase = true
    )
  }

  @Test
  fun `test character classes never ignore case`() {
    assertFailure(
      "IDEAVIM",
      "\\l\\+",
      ignoreCase = true
    )
  }

  @Test
  fun `test start of file`() {
    doTest(
      "${START}Idea${END}Vim",
      "\\%^Idea",
    )
  }

  @Test
  fun `test start of file should fail`() {
    assertFailure(
      "IdeaVim",
      "\\%^Vim",
    )
  }

  @Test
  fun `test end of file`() {
    doTest(
      "Idea${START}Vim$END",
      "Vim\\%$",
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
    doTest(
      "${START}IdeaVim$END",
      "\\%^IdeaVim\\%$",
    )
  }

  @Test
  fun `test for empty file`() {
    doTest(
      "$START$END",
      "\\v%^%$",
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
    doTest(
      """
        |Lorem Ipsum
        |
        |${START}Lorem$END ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "^Lorem",
      1
    )
  }

  @Test
  fun `test start of line should fail`() {
    assertFailure(
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "^Ipsum",
    )
  }

  @Test
  fun `test end of line`() {
    doTest(
      """
        |Lorem ${START}Ipsum$END
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "Ipsum$",
    )
  }

  @Test
  fun `test end of line should fail`() {
    assertFailure(
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "Lorem$"
    )
  }

  @Test
  fun `test start of line after alternation`() {
    doTest(
      "${START}dog$END barks",
      "^cat\\|^dog",
    )
  }

  @Test
  fun `test end of line before alternation`() {
    doTest(
      """
        |"${START}cat$END
        |"meows"
      """.trimMargin(),
      "cat$\\|dog$",
    )
  }

  @Test
  fun `test start and end of line inside parenthesis`() {
    doTest(
      "${START}cat meows$END",
      "\\v(^(cat|dog)) ((meows|barks)$)",
    )
  }

  @Test
  fun `test caret is taken literally`() {
    doTest(
      "${START}the symbol ^ is known as caret.$END",
      "^.\\+^.\\+$",
    )
  }

  @Test
  fun `test dollar sign is taken literally`() {
    doTest(
      "${START}the symbol for the dollar is $.$END",
      "^.\\+$.\\+$",
    )
  }

  @Test
  fun `test caret is taken literally at the start of pattern`() {
    doTest(
      "$START^ is known$END as caret.",
      "\\^ is known",
    )
  }

  @Test
  fun `test dollar sign is taken literally at the end of pattern`() {
    doTest(
      "the symbol for the ${START}dollar is $$END.",
      "dollar is \\$",
    )
  }

  @Test
  fun `test start of line anywhere in pattern`() {
    doTest(
      """
        |${START}Lorem Ipsum Lorem
        |
        |Lorem$END ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\_.\\+\\_^Lorem",
    )
  }

  @Test
  fun `test end of line anywhere in pattern`() {
    doTest(
      """
        |${START}Lorem Ipsum
        |
        |${END}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "Lorem Ipsum\\_$\\_s*",
    )
  }

  @Test
  fun `test atomic group 1`() {
    doTest(
      "${START}aaab$END",
      "\\(a*\\)\\@>b",
    )
  }

  @Test
  fun `test atomic group 2`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v.*(Lorem)@>",
    )
  }

  @Test
  fun `test atomic group should fail`() {
    /**
     * This pattern should fail because the "a*" consumes
     * all three 'a's, then the last "a" in the pattern
     * fails to match since all 'a's have been consumed.
     * Normally, it would try to backtrack and the "a*"
     * would only consume two 'a's, leaving the last one to
     * match with "a", but since the "a*" is atomic, it can't
     * try matching with shorter or longer sub-matches,
     * therefore the simulation immediately fails.
     */
    assertFailure(
      "aaa",
      "\\(a*\\)\\@>a"
    )
  }

  @Test
  fun `test collection with EOL`() {
    doTest(
      """
        |${START}Lorem Ipsum
        |
        |${END}123Lorem ipsum dolor sit amet, Lorem
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\_[a-z A-Z]\\+",
    )
  }

  @Test
  fun `test negated collection with EOL includes EOL anyway`() {
    doTest(
      """
      	|${START}Lorem Ipsum
        |
        |${END}123Lorem ipsum dolor sit amet, Lorem
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\_[^0-9]\\+",
    )
  }

  @Test
  fun `test collection decimal range`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "[\\d65-\\d122]*",
    )
  }

  @Test
  fun `test collection octal range`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "[\\o101-\\o172]*",
    )
  }

  @Test
  fun `test collection hex range`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "[\\x41-\\x7a]*",
    )
  }

  @Test
  fun `test collection unicode range`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "[\\u0041-\\u007a]*",
    )
  }

  @Test
  fun `test collection wide unicode range`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "[\\U00000041-\\U007a]*",
    )
  }

  @Test
  fun `test collection with escaped new line`() {
    doTest(
      "${START}Lorem Ipsum\n" +
        "Lorem ${END}123",
      "[\\n a-zA-Z]*",
    )
  }

  @Test
  fun `test collection with character class expression`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "[[:upper:][:lower:]]*",
    )
  }

  @Test
  fun `test collection with character class expression, range and single elements`() {
    doTest(
      "$START/unix/file/path/../path/.$END",
      "[-./[:alpha:]0-9_~]\\+",
    )
  }

  @Test
  fun `test positive lookahead 1`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "Lorem\\( Ipsum\\)\\@=",
    )
  }

  @Test
  fun `test positive lookahead 2`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "Lorem\\( Ipsum\\)\\@= Ipsum",
    )
  }

  @Test
  fun `test positive lookahead 3`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\vLorem( Ipsum)@=( Ipsum)@=( Ipsum)@=( Ipsum)@=( Ipsum)@=",
    )
  }

  @Test
  fun `test positive lookahead should fail 1`() {
    assertFailure(
      "Lorem Ipsum",
      "Lorem\\( Lorem\\)\\@="
    )
  }

  @Test
  fun `test positive lookahead should fail 2`() {
    assertFailure(
      "Lorem Ipsum Lorem",
      "Lorem\\( Ipsum\\)\\@= Lorem"
    )
  }

  @Test
  fun `test negative lookahead 1`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "Lorem\\( Lorem\\)\\@!",
    )
  }

  @Test
  fun `test negative lookahead 2`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "Lorem\\( Lorem\\)\\@! Ipsum",
    )
  }

  @Test
  fun `test negative lookahead 3`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\vLorem( Lorem)@!( Lorem)@!( Lorem)@!( Lorem)@!( Lorem)@!",
    )
  }

  @Test
  fun `test negative lookahead should fail 1`() {
    assertFailure(
      "Lorem Ipsum",
      "Lorem\\( Ipsum\\)\\@!"
    )
  }

  @Test
  fun `test negative lookahead should fail 2`() {
    assertFailure(
      "Lorem Ipsum",
      "\\vLorem( Lorem)@!( Lorem)@!( Lorem)@!( Ipsum)@!( Lorem)@!"
    )
  }

  @Test
  fun `test double negative lookahead equals a positive`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\vLorem(( Ipsum)@!)@!",
    )
  }

  @Test
  fun `test double positive lookahead equals a positive`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\vLorem(( Ipsum)@=)@=",
    )
  }

  @Test
  fun `test positive and negative lookahead equals a negative`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\vLorem(( Lorem)@=)@!",
    )
  }

  @Test
  fun `test negative and positive lookahead equals a negative`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\vLorem(( Lorem)@!)@=",
    )
  }

  @Test
  fun `test negative and positive lookahead equals a negative and fails`() {
    assertFailure(
      "Lorem Ipsum",
      "\\vLorem(( Ipsum)@!)@="
    )
  }

  @Test
  fun `test positive lookahead with nested capturing groups`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v(Lorem( Ipsum)@=)",
    )
  }

  @Test
  fun `test positive lookahead with multiple conditions`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "\\vLorem( Ipsum)@=( XYZ| Ipsum)",
    )
  }

  @Test
  fun `test negative lookahead with nested capturing groups`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v(Lorem( XYZ)@!)",
    )
  }

  @Test
  fun `test negative lookahead with multiple conditions`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "\\vLorem( XYZ)@!( XYZ| Ipsum)",
    )
  }

  @Test
  fun `test combination of positive and negative lookahead`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "\\vLorem( Ipsum)@=( Ipsum( Lorem)@!)",
    )
  }

  @Test
  fun `test AND operator 1`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "Lorem Ipsum\\&.....",
    )
  }

  @Test
  fun `test AND operator 2`() {
    doTest(
      "${START}Lorem Ipsum$END",
      ".*Ip\\&.*sum",
    )
  }

  @Test
  fun `test multiple AND operators`() {
    doTest(
      "${START}Lorem$END Ipsum",
      ".*Ip\\&.*sum\\&Lorem Ipsum\\&Lorem",
    )
  }

  @Test
  fun `test AND operator inside group followed by word`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "\\v(Lorem&.*) Ipsum",
    )
  }

  @Test
  fun `test AND operator inside group correct capture`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v(Lorem&.*) Ipsum",
      groupNumber = 1
    )
  }

  @Test
  fun `test AND operator should fail`() {
    assertFailure(
      "Lorem Ipsum",
      "Ipsum\\&Lorem"
    )
  }

  @Test
  fun `test positive lookbehind 1`() {
    doTest(
      "Lorem ${START}Ipsum$END",
      "\\v(Lorem )@<=Ipsum",
    )
  }

  @Test
  fun `test positive lookbehind 2`() {
    doTest(
      "Lor${START}e${END}m Ipsum",
      "\\v(\\w{3})@<=\\w",
    )
  }

  @Test
  fun `test positive lookbehind 3`() {
    doTest(
      "Lorem     ${START}Ipsum$END",
      "\\v(\\s+)@<=\\w+",
    )
  }

  @Test
  fun `test positive lookbehind should fail 1`() {
    assertFailure(
      "Lorem Ipsum",
      "\\v(Lorem)@<=Lorem"
    )
  }

  @Test
  fun `test positive lookbehind should fail 2`() {
    assertFailure(
      "Lorem Ipsum",
      "\\v(Lorem )@<=(Lorem )@<=(Ipsum )@<=(Lorem )@<=Ipsum"
    )
  }

  @Test
  fun `test negative lookbehind 1`() {
    doTest(
      "Lorem ${START}Ipsum$END",
      "\\v(Ipsum)@<!Ipsum",
    )
  }

  @Test
  fun `test negative lookbehind 2`() {
    doTest(
      "${START}Lorem Ipsum$END",
      "\\vLorem( Ipsum)@<! Ipsum",
    )
  }

  @Test
  fun `test negative lookbehind 3`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\v( Lorem)@<!( Lorem)@<!( Lorem)@<!( Lorem)@<!( Lorem)@<!Lorem",
    )
  }

  @Test
  fun `test negative lookbehind should fail 1`() {
    assertFailure(
      "Lorem Ipsum",
      "\\vLorem(Lorem)@<! Ipsum"
    )
  }

  @Test
  fun `test negative lookbehind should fail 2`() {
    assertFailure(
      "Lorem Ipsum",
      "\\v(XYZ)@<!(XYZ)@<!(XYZ)@<!( )@<!(XYZ)@<!Ipsum"
    )
  }

  @Test
  fun `test limited lookbehind doesn't go out of bounds`() {
    doTest(
      "Lorem ${START}Ipsum$END",
      "\\v(Lorem )@10000<=Ipsum",
    )
  }

  @Test
  fun `test limited positive lookbehind succeeds`() {
    doTest(
      "abbcab${START}c$END",
      "\\v(a.*)@2<=c",
    )
  }

  @Test
  fun `test limited negative lookbehind succeeds`() {
    doTest(
      "abb${START}c${END}abc",
      "\\v(a.*)@2<!c",
    )
  }

  @Test
  fun `test limited lookbehind fails because of small limit`() {
    assertFailure(
      "Lorem Ipsum",
      "\\v(Lorem )@1<=Ipsum"
    )
  }

  @Test
  fun `test match character by decimal code`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\%d76orem",
    )
  }

  @Test
  fun `test match character by non-ascii decimal code`() {
    doTest(
      "${START}Гorem$END Ipsum",
      "\\%d1043orem",
    )
  }

  @Test
  fun `test match character by decimal code should fail`() {
    assertFailure(
      "Lorem Ipsum",
      "\\%d77orem"
    )
  }

  @Test
  fun `test match character by octal code`() {
    doTest(
      "${START}Lorem$END Ipsum",
      "\\%o114orem",
    )
  }

  @Test
  fun `test match character by large octal code`() {
    /**
     * Since octal codes can only go up to 377, the
     * 400 in this pattern is actually not the code
     * of the matched character; instead, it matches
     * the character with octal code 40 (space) followed
     * by a '0'
     */
    doTest(
      "$START 0${END}123",
      "\\%o400",
    )
  }

  @Test
  fun `test match character by hexadecimal code`() {
    /**
     * Match character with code 0x31, followed by '23'
     */
    doTest(
      "${START}123$END",
      "\\%x3123",
    )
  }

  @Test
  fun `test match character by 4 long hexadecimal code fails`() {
    /**
     * Match character with code 0x3123
     */
    assertFailure(
      "123",
      "\\%u3123"
    )
  }

  @Test
  fun `test match character by 4 long hexadecimal code`() {
    /**
     * Match character with code 0x31 followed by '23'
     */
    doTest(
      "${START}123$END",
      "\\%u003123",
    )
  }

  @Test
  fun `test match character by 8 long hexadecimal code`() {
    /**
     * Match character with code 0x31 followed by '23'
     */
    doTest(
      "${START}123$END",
      "\\%U0000003123",
    )
  }

  @Test
  fun `test match characters at line 3`() {
    doTest(
      """
      	|Lorem Ipsum
        |
        |${START}Lorem$END ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%3lLorem",
    )
  }

  @Test
  fun `test match characters before line 3`() {
    doTest(
      """
      	|${START}Lorem$END Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%<3lLorem",
    )
  }

  @Test
  fun `test match characters after line 2`() {
    doTest(
      """
      	|Lorem Ipsum
        |
        |${START}Lorem$END ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%>2lLorem",
    )
  }

  @Test
  fun `test match character at column 11`() {
    doTest(
      """
      	|Lorem Ipsu${START}m$END
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%11cm",
    )
  }

  @Test
  fun `test match characters before column 11`() {
    doTest(
      """
      	|Lore${START}m$END Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%<11cm"
    )
  }

  @Test
  fun `test match characters after column 6`() {
    doTest(
      """
      	|Lorem Ipsu${START}m$END
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%>6cm"
    )
  }

  @Test
  fun `test match characters at cursor line`() {
    doTest(
      """
      	|Lorem Ipsum
        |
        |${START}Lor${END}em${CARET} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%.l...",
    )
  }

  @Test
  fun `test match characters before cursor line`() {
    doTest(
      """
      	|${START}Lor${END}em Ipsum
        |
        |Lorem${CARET} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%<.l..."
    )
  }

  @Test
  fun `test match characters after cursor line`() {
    doTest(
      """
      	|Lorem Ipsum
        |
        |Lorem${CARET} ipsum dolor sit amet,
        |${START}con${END}sectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%>.l..."
    )
  }

  @Test
  fun `test match characters at cursor column`() {
    doTest(
      """
      	|Lorem$START Ip${END}sum
        |
        |Lorem${CARET} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%.c..."
    )
  }

  @Test
  fun `test match characters before cursor column`() {
    doTest(
      """
      	|${START}Lor${END}em Ipsum
        |
        |Lorem${CARET} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%<.c..."
    )
  }

  @Test
  fun `test match characters after cursor column`() {
    doTest(
      """
      	|Lorem ${START}Ips${END}um
        |
        |Lorem${CARET} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      "\\%>.c..."
    )
  }

  @Test
  fun `test optionally matched characters 1`() {
    doTest(
      "${START}substitute${END}",
      "s\\%[ubstitute]"
    )
  }

  @Test
  fun `test optionally matched characters 2`() {
    doTest(
      "${START}sub${END}",
      "s\\%[ubstitute]"
    )
  }

  @Test
  fun `test optionally matched characters 3`() {
    doTest(
      "${START}s${END}",
      "s\\%[ubstitute]"
    )
  }

  @Test
  fun `test optionally matched sequence with collection 1`() {
    doTest(
      "${START}read${END}",
      "r\\%[[eo]ad]"
    )
  }

  @Test
  fun `test optionally matched sequence with collection 2`() {
    doTest(
      "${START}road${END}",
      "r\\%[[eo]ad]"
    )
  }

  @Test
  fun `test optionally matched sequence with collection 3`() {
    doTest(
      "${START}rea${END}",
      "r\\%[[eo]ad]"
    )
  }

  @Test
  fun `test optionally matched sequence with collection 4`() {
    doTest(
      "${START}roa${END}",
      "r\\%[[eo]ad]"
    )
  }

  @Test
  fun `test optionally matched sequence with escaped brackets 1`() {
    doTest(
      "${START}index[0]${END}",
      "index\\%[\\[0\\]]"
    )
  }

  @Test
  fun `test optionally matched sequence with escaped brackets 2`() {
    doTest(
      "${START}index[${END}",
      "index\\%[\\[0\\]]"
    )
  }

  @Test
  fun `test optionally matched sequence with group 1`() {
    doTest(
      "${START}function${END}",
      "\\vf%[(un)ction]"
    )
  }

  @Test
  fun `test optionally matched sequence with group 2`() {
    doTest(
      "${START}f${END}u",
      "\\vf%[(un)ction]"
    )
  }

  @Test
  fun `test mark does not exist`() {
    assertFailure(
      "Lorem ${MARK('m')}Ipsum",
      "\\%'n..."
    )
  }

  @Test
  fun `test pattern with multiple cursors at different indexes fails`() {
    assertFailure(
      "${CARET}Lorem ${CARET}Ipsum",
      "\\%#.\\+\\%#"
    )
  }

  @Test
  fun `test pattern with single dollar sign`() {
    doTest(
      "Lorem\$Ipsum${START}${END}",
      "$"
    )
  }

  @Test
  fun `test pattern with single caret symbol`() {
    doTest(
      "${START}${END}Lorem^Ipsum",
      "^"
    )
  }

  companion object {
    private fun assertFailure(
      text: CharSequence,
      pattern: String,
      offset: Int = 0,
      ignoreCase: Boolean = false,
    ) {
      val editor = mockEditorFromText(text)
      val nfa = buildNFA(pattern)
      assertTrue(VimRegexEngine.simulate(nfa, editor, offset, ignoreCase) is VimMatchResult.Failure)
    }

    private fun doTest(
      text: CharSequence,
      pattern: String,
      offset: Int = 0,
      ignoreCase: Boolean = false,
      groupNumber: Int = 0,
    ) {
      val editor = mockEditorFromText(text)
      val nfa = buildNFA(pattern)

      val result = VimRegexEngine.simulate(nfa, editor, offset, ignoreCase)
      when (result) {
        is VimMatchResult.Success -> assertEquals(
          getMatchRanges(text).firstOrNull(),
          result.groups.get(groupNumber)?.range
        )

        else -> fail("Expected to find match")
      }
    }

    private fun doTest(
      text: CharSequence,
      pattern: String,
      carets: List<VimCaret>,
      offset: Int = 0,
      ignoreCase: Boolean = false,
      groupNumber: Int = 0,
    ) {
      val editor = mockEditor(text, carets)
      val nfa = buildNFA(pattern)

      val result = VimRegexEngine.simulate(nfa, editor, offset, ignoreCase)
      when (result) {
        is VimMatchResult.Success -> assertEquals(
          getMatchRanges(text).firstOrNull(),
          result.groups.get(groupNumber)?.range
        )

        else -> fail("Expected to find match")
      }
    }

    private fun buildNFA(pattern: String): NFA {
      val parserResult = VimRegexParser.parse(pattern)
      return when (parserResult) {
        is VimRegexParserResult.Failure -> fail("Parsing failed")
        is VimRegexParserResult.Success -> NFA.fromMatcher(DotMatcher(true)).closure(false)
          .concatenate(PatternVisitor.visit(parserResult.tree))
      }
    }
  }
}