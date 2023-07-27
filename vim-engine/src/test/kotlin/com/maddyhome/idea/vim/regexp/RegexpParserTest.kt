/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.RegexParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import kotlin.test.fail


class RegexpParserTest {
  @Test
  fun `range both bounds`() {
    assertSuccess("\\{2,5}", RANGE)
  }

  @Test
  fun `range left bound`() {
    assertSuccess("\\{6,}", RANGE)
  }

  @Test
  fun `range right bound`() {
    assertSuccess("\\{,10}", RANGE)
  }

  @Test
  fun `range absolute bound`() {
    assertSuccess("\\{5}", RANGE)
  }

  @Test
  fun `range missing right bracket`() {
    assertFailure("\\{5", RANGE)
  }

  @Test
  fun `range missing left bracket`() {
    assertFailure("2,5}", RANGE)
  }

  @Test
  fun `range two commas`() {
    assertFailure("\\{2,5,}", RANGE)
  }

  @Test
  fun `range non integer bound`() {
    assertFailure("\\{2,g}", RANGE)
  }

  @Test
  fun `collection a to z`() {
    assertSuccess("[a-z]", COLLECTION)
  }

  @Test
  fun `collection 0 to 9`() {
    assertSuccess("[0-9]", COLLECTION)
  }
  @Test
  fun `collection single element`() {
    assertSuccess("[f]", COLLECTION)
  }

  @Test
  fun `collection a to z and A`() {
    assertSuccess("[a-zA]", COLLECTION)
  }

  @Test
  fun `collection a to z and A to Z`() {
    assertSuccess("[a-zA-Z]", COLLECTION)
  }

  @Test
  fun `collection a to z, 9 and A to Z`() {
    assertSuccess("[a-z9A-Z]", COLLECTION)
  }

  @Test
  fun `collection a to z, dash and Z`() {
    /**
     * This pattern looks like it should
     * be illegal, but Vim allows it and
     * matches the characters 'a' to 'z', a
     * literal dash '-' and a 'Z'
     */
    assertSuccess("[a-z-Z]", COLLECTION)
  }

  @Test
  fun `collection with single dash`() {
    assertSuccess("[-]", COLLECTION)
  }

  @Test
  fun `collection dash to 0`() {
    assertSuccess("[--0]", COLLECTION)
  }

  @Test
  fun `collection literal dash and a to z`() {
    assertSuccess("[-a-z]", COLLECTION)
  }

  @Test
  fun `collection a to z and literal dash`() {
    assertSuccess("[a-z-]", COLLECTION)
  }

  @Test
  fun `collection a, literal dash and b`() {
    assertSuccess("[a\\-b]", COLLECTION)
  }

  @Test
  fun `collection escaped backslash`() {
    assertSuccess("[\\\\]", COLLECTION)
  }

  @Test
  fun `collection a to z negated`() {
    assertSuccess("[^a-z]", COLLECTION)
  }

  @Test
  fun `unclosed collection`() {
    assertFailure("[a-z", COLLECTION)
  }

  @Test
  fun `'ab'`() {
    assertSuccess("ab", PATTERN)
  }

  @Test
  fun `sequence of 0 or more 'ab'`() {
    assertSuccess("\\(ab\\)*", PATTERN)
  }

  @Test
  fun `sequence of 0 or more 'ab' no magic`() {
    assertSuccess("\\M\\(ab\\)\\*", PATTERN)
  }

  @Test
  fun `sequence of 1 or more 'ab'`() {
    assertSuccess("\\(ab\\)\\+", PATTERN)
  }

  @Test
  fun `0 or 1 'ab' with equals`() {
    assertSuccess("\\(ab\\)\\=", PATTERN)
  }

  @Test
  fun `0 or 1 'ab' with question mark`() {
    assertSuccess("\\(ab\\)\\?", PATTERN)
  }

  @Test
  fun `nested groups with multi`() {
    assertSuccess("\\(\\(a\\)*b\\)\\+", PATTERN)
  }

  @Test
  fun `non-capture group`() {
    assertSuccess("\\%(a\\)", PATTERN)
  }

  @Test
  fun `very nomagic characters`() {
    assertSuccess("\\V%(", PATTERN)
  }

  @Test
  fun `date format`() {
    assertSuccess("\\(\\d\\{2}\\)\\{2}\\d\\{4}", PATTERN)
  }

  @Test
  fun `switching to nomagic`() {
    assertSuccess("a*\\Ma*", PATTERN)
  }

  @Test
  fun `switching to all magic modes`() {
    assertSuccess("\\m.*\\M\\.\\*\\v.*\\V\\.\\*", PATTERN)
  }

  @Test
  fun `unclosed group`() {
    assertFailure("\\(ab", PATTERN)
  }

  @Test
  fun `unmatched closing )`() {
    assertFailure("ab\\)", PATTERN)
  }

  @Test
  fun `unclosed non-capture group`() {
    assertFailure("\\%(a", PATTERN)
  }

  @Test
  fun `unescaped group close`() {
    assertFailure("\\(a)", PATTERN)
  }

  @Test
  fun `invalid very magic character`() {
    assertFailure("\\v%", PATTERN)
  }

  private fun generateParser(pattern: String): RegexParser {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    return RegexParser(tokens)
  }

  private fun assertSuccess(pattern: String, startSymbol : String) {
    val parser = generateParser(pattern)
    parser.javaClass.getMethod(startSymbol).invoke(parser)
  }

  private fun assertFailure(pattern: String, startSymbol: String) {
    try {
      val parser = generateParser(pattern)
      parser.javaClass.getMethod(startSymbol).invoke(parser)
    } catch (exception: InvocationTargetException) {
      exception.printStackTrace()
      return
    }
    fail("Pattern $pattern should fail for rule $startSymbol")
  }

  companion object {
    private const val PATTERN = "pattern"
    private const val COLLECTION = "collection"
    private const val RANGE = "range"
  }
}
