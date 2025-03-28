/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoesNotMatchOperatorTest : VimTestCase() {
  @Test
  fun `test does not match operator returns false when pattern matches string`() {
    assertFalse(evaluate("'lorem ipsum' !~ 'l*sum'").asBoolean())
  }

  @Test
  fun `test does not match operator returns true when pattern does not match string`() {
    assertTrue(evaluate("'lorem ipsum' !~ 'l*foo'").asBoolean())
  }

  @Test
  fun `test does not match operator returns false when pattern matches case`() {
    assertFalse(evaluate("'Lorem Ipsum' !~ 'L*I'").asBoolean())
  }

  @Test
  fun `test does not match operator returns true when pattern does not match case`() {
    assertTrue(evaluate("'Lorem Ipsum' !~ 'l*i'").asBoolean())
  }

  @Test
  fun `test does not match operator with 'noignorecase' returns false with different case pattern`() {
    injector.globalOptions().ignorecase = true // Default is false
    assertFalse(evaluate("'Lorem Ipsum' !~ 'l*i'").asBoolean())
  }

  // Case-sensitive operator
  @Test
  fun `test case-sensitive does not match operator returns false when pattern matches string`() {
    assertFalse(evaluate("'lorem ipsum' !~# 'l*sum'").asBoolean())
  }

  @Test
  fun `test case-sensitive does not match operator returns true when pattern does not match string`() {
    assertTrue(evaluate("'lorem ipsum' !~# 'l*foo'").asBoolean())
  }

  @Test
  fun `test case-sensitive does not match operator returns true when pattern does not match string case`() {
    assertTrue(evaluate("'lorem ipsum' !~# 'L*Sum'").asBoolean())
  }

  // Case-insensitive operator
  @Test
  fun `test case-insensitive does not match operator returns false when pattern matches string`() {
    assertFalse(evaluate("'lorem ipsum' !~? 'l*sum'").asBoolean())
  }

  @Test
  fun `test case-insensitive does not match operator returns true when pattern does not match string`() {
    assertTrue(evaluate("'lorem ipsum' !~? 'l*foo'").asBoolean())
  }

  @Test
  fun `test case-insensitive does not match operator returns false when pattern does not match string case`() {
    assertFalse(evaluate("'lorem ipsum' !~? 'L*Sum'").asBoolean())
  }

  private fun evaluate(expression: String) = VimscriptParser.parseExpression(expression)!!.evaluate()
}
