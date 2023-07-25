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
  private val PATTERN = "pattern"
  private val RANGE = "range"
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
  fun `single char with range multi`() {
    assertSuccess("a\\{1,3}", PATTERN)
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
}
