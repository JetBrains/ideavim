/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import org.junit.jupiter.api.Test
import kotlin.test.fail


class VimRegexParserTest {
  @Test
  fun `range both bounds`() {
    assertSuccess("a\\{2,5}")
  }

  @Test
  fun `range left bound`() {
    assertSuccess("a\\{6,}")
  }

  @Test
  fun `range right bound`() {
    assertSuccess("a\\{,10}")
  }

  @Test
  fun `range absolute bound`() {
    assertSuccess("a\\{5}")
  }

  @Test
  fun `range lazy`() {
    assertSuccess("a\\{-,5}")
  }

  @Test
  fun `range missing right bracket`() {
    assertFailure("a\\{5")
  }

  @Test
  fun `range two commas`() {
    assertFailure("a\\{2,5,}")
  }

  @Test
  fun `range non integer bound`() {
    assertFailure("a\\{2,g}")
  }

  @Test
  fun `range lazy with extra dash`() {
    assertFailure("a\\{--2,5}")
  }

  @Test
  fun `collection a to z`() {
    assertSuccess("[a-z]")
  }

  @Test
  fun `collection 0 to 9`() {
    assertSuccess("[0-9]")
  }
  @Test
  fun `collection single element`() {
    assertSuccess("[f]")
  }

  @Test
  fun `collection a to z and A`() {
    assertSuccess("[a-zA]")
  }

  @Test
  fun `collection a to z and A to Z`() {
    assertSuccess("[a-zA-Z]")
  }

  @Test
  fun `collection a to z, 9 and A to Z`() {
    assertSuccess("[a-z9A-Z]")
  }

  @Test
  fun `collection a to z, dash and Z`() {
    /**
     * This pattern looks like it should
     * be illegal, but Vim allows it and
     * matches the characters 'a' to 'z', a
     * literal dash '-' and a 'Z'
     */
    assertSuccess("[a-z-Z]")
  }

  @Test
  fun `collection with single dash`() {
    assertSuccess("[-]")
  }

  @Test
  fun `collection dash to 0`() {
    assertSuccess("[--0]")
  }

  @Test
  fun `collection literal dash and a to z`() {
    assertSuccess("[-a-z]")
  }

  @Test
  fun `collection a to z and literal dash`() {
    assertSuccess("[a-z-]")
  }

  @Test
  fun `collection a, literal dash and b`() {
    assertSuccess("[a\\-b]")
  }

  @Test
  fun `collection escaped backslash`() {
    assertSuccess("[\\\\]")
  }

  @Test
  fun `collection a to z negated`() {
    assertSuccess("[^a-z]")
  }

  @Test
  fun `collection with negated unescaped caret`() {
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
  fun `collection with escaped caret`() {
    assertSuccess("[\\^]")
  }

  @Test
  fun `collection unescaped backslash not at end`() {
    /**
     * Matches a "\" or "a".
     * Since "\a" isn't an escape sequence,
     * the "\" is taken literally.
     * Equivalent to "[\\a]]"
     */
    assertSuccess("[\\a]")
  }

  @Test
  fun `collection unicode code range`() {
    assertSuccess("[\\u0-\\uFFFF]")
  }

  @Test
  fun `collection russian alphabet`() {
    assertSuccess("[А-яЁё]")
  }

  @Test
  fun `unclosed collection`() {
    assertFailure("[a-z")
  }

  @Test
  fun `collection unescaped backslash at end`() {
    assertFailure("[abc\\]")
  }

  @Test
  fun `unicode character`() {
    assertSuccess("\u03b5")
  }

  @Test
  fun `unicode character in nomagic mode`() {
    assertSuccess("\\M\u03b5")
  }

  @Test
  fun `wider unicode character`() {
    assertSuccess("\uD83E\uDE24")
  }

  @Test
  fun `'ab'`() {
    assertSuccess("ab")
  }

  @Test
  fun `'ab' after cursor`() {
    assertSuccess("\\%#ab")
  }

  @Test
  fun `sequence of 0 or more 'ab'`() {
    assertSuccess("\\(ab\\)*")
  }

  @Test
  fun `sequence of 0 or more 'ab' no magic`() {
    assertSuccess("\\M\\(ab\\)\\*")
  }

  @Test
  fun `sequence of 1 or more 'ab'`() {
    assertSuccess("\\(ab\\)\\+")
  }

  @Test
  fun `0 or 1 'ab' with equals`() {
    assertSuccess("\\(ab\\)\\=")
  }

  @Test
  fun `0 or 1 'ab' with question mark`() {
    assertSuccess("\\(ab\\)\\?")
  }

  @Test
  fun `nested groups with multi`() {
    assertSuccess("\\(\\(a\\)*b\\)\\+")
  }

  @Test
  fun `non-capture group`() {
    assertSuccess("\\%(a\\)")
  }

  @Test
  fun `very nomagic characters`() {
    assertSuccess("\\V%(")
  }

  @Test
  fun `date format`() {
    assertSuccess("\\(\\d\\{2}\\)\\{2}\\d\\{4}")
  }

  @Test
  fun `switching to nomagic`() {
    assertSuccess("a*\\Ma*")
  }

  @Test
  fun `switching to all magic modes`() {
    assertSuccess("\\m.*\\M\\.\\*\\v.*\\V\\.\\*")
  }

  @Test
  fun `backreference to group 1`() {
    assertSuccess("\\v(cat|dog)\\1")
  }

  @Test
  fun `unclosed group`() {
    assertFailure("\\(ab")
  }

  @Test
  fun `unmatched closing )`() {
    assertFailure("ab\\)")
  }

  @Test
  fun `unclosed non-capture group`() {
    assertFailure("\\%(a")
  }

  @Test
  fun `unescaped group close`() {
    assertFailure("\\(a)")
  }

  private fun assertSuccess(pattern: String) {
    val parser = VimRegexParser(pattern)
    val result = parser.parse()
    if (parser.parse() is VimRegexParserResult.Failure) {
      fail("Expecting successful parsing for pattern $pattern but got ${(result as VimRegexParserResult.Failure).message}")
    }
  }

  private fun assertFailure(pattern: String) {
    val parser = VimRegexParser(pattern)
    if (parser.parse() is VimRegexParserResult.Success) {
      fail("Expecting unsuccessful parsing for pattern $pattern")
    }
  }
}
