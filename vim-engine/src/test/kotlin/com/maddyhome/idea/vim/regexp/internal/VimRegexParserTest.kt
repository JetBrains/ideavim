/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.internal

import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import org.junit.jupiter.api.Test
import kotlin.test.fail


class VimRegexParserTest {
  @Test
  fun `test range both bounds`() {
    assertSuccess("a\\{2,5}")
  }

  @Test
  fun `test range left bound`() {
    assertSuccess("a\\{6,}")
  }

  @Test
  fun `test range right bound`() {
    assertSuccess("a\\{,10}")
  }

  @Test
  fun `test range absolute bound`() {
    assertSuccess("a\\{5}")
  }

  @Test
  fun `test range lazy`() {
    assertSuccess("a\\{-,5}")
  }

  @Test
  fun `test range missing right bracket`() {
    assertFailure("a\\{5")
  }

  @Test
  fun `test range two commas`() {
    assertFailure("a\\{2,5,}")
  }

  @Test
  fun `test range non integer bound`() {
    assertFailure("a\\{2,g}")
  }

  @Test
  fun `test range lazy with extra dash`() {
    assertFailure("a\\{--2,5}")
  }

  @Test
  fun `test collection a to z`() {
    assertSuccess("[a-z]")
  }

  @Test
  fun `test collection 0 to 9`() {
    assertSuccess("[0-9]")
  }

  @Test
  fun `test collection single element`() {
    assertSuccess("[f]")
  }

  @Test
  fun `test collection a to z and A`() {
    assertSuccess("[a-zA]")
  }

  @Test
  fun `test collection a to z and A to Z`() {
    assertSuccess("[a-zA-Z]")
  }

  @Test
  fun `test collection a to z, 9 and A to Z`() {
    assertSuccess("[a-z9A-Z]")
  }

  @Test
  fun `test collection a to z, dash and Z`() {
    /**
     * This pattern looks like it should
     * be illegal, but Vim allows it and
     * matches the characters 'a' to 'z', a
     * literal dash '-' and a 'Z'
     */
    assertSuccess("[a-z-Z]")
  }

  @Test
  fun `test collection with single dash`() {
    assertSuccess("[-]")
  }

  @Test
  fun `test collection dash to 0`() {
    assertSuccess("[--0]")
  }

  @Test
  fun `test collection literal dash and a to z`() {
    assertSuccess("[-a-z]")
  }

  @Test
  fun `test collection a to z and literal dash`() {
    assertSuccess("[a-z-]")
  }

  @Test
  fun `test collection a, literal dash and b`() {
    assertSuccess("[a\\-b]")
  }

  @Test
  fun `test collection escaped backslash`() {
    assertSuccess("[\\\\]")
  }

  @Test
  fun `test collection a to z negated`() {
    assertSuccess("[^a-z]")
  }

  @Test
  fun `test collection with negated unescaped caret`() {
    /**
     * Matches everything except "^".
     * It's more correct to write it as
     * "[^\^]", escaping the "^", but "^"
     * is still allowed to be unescaped and
     * taken literally when not immediately
     * after the "["
     */
    assertSuccess("[^^]")
  }

  @Test
  fun `test collection with escaped caret`() {
    assertSuccess("[\\^]")
  }

  @Test
  fun `test collection unescaped backslash not at end`() {
    /**
     * Matches a "\" or "a".
     * Since "\a" isn't an escape sequence,
     * the "\" is taken literally.
     * Equivalent to "[\\a]"
     */
    assertSuccess("[\\a]")
  }

  @Test
  fun `test collection unicode code range`() {
    assertSuccess("[\\u0-\\uFFFF]")
  }

  @Test
  fun `test collection russian alphabet`() {
    assertSuccess("[А-яЁё]")
  }

  @Test
  fun `test unclosed collection`() {
    assertFailure("[a-z")
  }

  @Test
  fun `test collection unescaped backslash at end`() {
    assertFailure("[abc\\]")
  }

  @Test
  fun `test collection with character class expression`() {
    assertSuccess("[[:alpha:]]")
  }

  @Test
  fun `test collection with invalid character class expression`() {
    /**
     * Although "[:invalid:]" doesn't correspond to any character
     * class expression, this pattern is still valid and means:
     * Any of these characters: '[' ':' 'i' 'n' 'v' 'a' 'l' 'd',
     * followed by a ']'
     */
    assertSuccess("[[:invalid:]]")
  }

  @Test
  fun `test collection with character class expression missing closing bracket`() {
    assertFailure("[[:alnum:]")
  }

  @Test
  fun `test collection with character class expression and other elements`() {
    assertSuccess("[a-z[:digit:]-Z]")
  }

  @Test
  fun `test opening bracket followed by collection`() {
    assertSuccess("\\[[a-z]")
  }

  @Test
  fun `test collection with opening bracket`() {
    assertSuccess("[[a-z]")
  }

  @Test
  fun `test unicode character`() {
    assertSuccess("\u03b5")
  }

  @Test
  fun `test unicode character in nomagic mode`() {
    assertSuccess("\\M\u03b5")
  }

  @Test
  fun `test wider unicode character`() {
    assertSuccess("\uD83E\uDE24")
  }

  @Test
  fun `test 'ab'`() {
    assertSuccess("ab")
  }

  @Test
  fun `test 'ab' after cursor`() {
    assertSuccess("\\%#ab")
  }

  @Test
  fun `test sequence of 0 or more 'ab'`() {
    assertSuccess("\\(ab\\)*")
  }

  @Test
  fun `test sequence of 0 or more 'ab' no magic`() {
    assertSuccess("\\M\\(ab\\)\\*")
  }

  @Test
  fun `test sequence of 1 or more 'ab'`() {
    assertSuccess("\\(ab\\)\\+")
  }

  @Test
  fun `test 0 or 1 'ab' with equals`() {
    assertSuccess("\\(ab\\)\\=")
  }

  @Test
  fun `test 0 or 1 'ab' with question mark`() {
    assertSuccess("\\(ab\\)\\?")
  }

  @Test
  fun `test nested groups with multi`() {
    assertSuccess("\\(\\(a\\)*b\\)\\+")
  }

  @Test
  fun `test non-capture group`() {
    assertSuccess("\\%(a\\)")
  }

  @Test
  fun `test very nomagic characters`() {
    assertSuccess("\\V%(")
  }

  @Test
  fun `test date format`() {
    assertSuccess("\\(\\d\\{2}\\)\\{2}\\d\\{4}")
  }

  @Test
  fun `test switching to nomagic`() {
    assertSuccess("a*\\Ma*")
  }

  @Test
  fun `test switching to all magic modes`() {
    assertSuccess("\\m.*\\M\\.\\*\\v.*\\V\\.\\*")
  }

  @Test
  fun `test backreference to group 1`() {
    assertSuccess("\\v(cat|dog)\\1")
  }

  @Test
  fun `test unclosed group`() {
    assertFailure("\\(ab")
  }

  @Test
  fun `test unmatched closing )`() {
    assertFailure("ab\\)")
  }

  @Test
  fun `test unclosed non-capture group`() {
    assertFailure("\\%(a")
  }

  @Test
  fun `test unescaped group close`() {
    assertFailure("\\(a)")
  }

  private fun assertSuccess(pattern: String) {
    val result = VimRegexParser.parse(pattern)
    if (result is VimRegexParserResult.Failure) {
      fail("Expecting successful parsing for pattern $pattern but got ${result.errorCode}")
    }
  }

  private fun assertFailure(pattern: String) {
    if (VimRegexParser.parse(pattern) is VimRegexParserResult.Success) {
      fail("Expecting unsuccessful parsing for pattern $pattern")
    }
  }
}
