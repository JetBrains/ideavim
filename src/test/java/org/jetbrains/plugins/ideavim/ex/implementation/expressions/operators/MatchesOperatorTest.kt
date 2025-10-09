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

class MatchesOperatorTest : VimTestCase() {
  @Test
  fun `test matches operator returns true when pattern matches string`() {
    assertTrue(evaluate("'lorem ipsum' =~ 'l*sum'").toVimNumber().booleanValue)
  }

  @Test
  fun `test matches operator returns false when pattern does not match string`() {
    assertFalse(evaluate("'lorem ipsum' =~ 'l*foo'").toVimNumber().booleanValue)
  }

  @Test
  fun `test matches operator returns true when pattern matches case`() {
    assertTrue(evaluate("'Lorem Ipsum' =~ 'L*I'").toVimNumber().booleanValue)
  }

  @Test
  fun `test matches operator returns false when pattern does not match case`() {
    assertFalse(evaluate("'Lorem Ipsum' =~ 'l*i'").toVimNumber().booleanValue)
  }

  @Test
  fun `test matches operator with 'noignorecase' returns true with different case pattern`() {
    injector.globalOptions().ignorecase = true // Default is false
    assertTrue(evaluate("'Lorem Ipsum' =~ 'l*i'").toVimNumber().booleanValue)
  }

  // Case-sensitive operator
  @Test
  fun `test case-sensitive matches operator returns true when pattern matches string`() {
    assertTrue(evaluate("'lorem ipsum' =~# 'l*sum'").toVimNumber().booleanValue)
  }

  @Test
  fun `test case-sensitive matches operator returns false when pattern does not match string`() {
    assertFalse(evaluate("'lorem ipsum' =~# 'l*foo'").toVimNumber().booleanValue)
  }

  @Test
  fun `test case-sensitive matches operator returns false when pattern does not match string case`() {
    assertFalse(evaluate("'lorem ipsum' =~# 'L*Sum'").toVimNumber().booleanValue)
  }

  // Case-insensitive operator
  @Test
  fun `test case-insensitive matches operator returns true when pattern matches string`() {
    assertTrue(evaluate("'lorem ipsum' =~? 'l*sum'").toVimNumber().booleanValue)
  }

  @Test
  fun `test case-insensitive matches operator returns false when pattern does not match string`() {
    assertFalse(evaluate("'lorem ipsum' =~? 'l*foo'").toVimNumber().booleanValue)
  }

  @Test
  fun `test case-insensitive matches operator returns true when pattern does not match string case`() {
    assertTrue(evaluate("'lorem ipsum' =~? 'L*Sum'").toVimNumber().booleanValue)
  }

  private fun evaluate(expression: String) = VimscriptParser.parseExpression(expression)!!.evaluate()
}
