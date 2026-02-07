/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GreaterOrEqualsOperatorTest : VimTestCase() {

  @Test
  fun `test equal numbers`() {
    assertEquals(VimInt.ONE, VimscriptParser.parseExpression("1 >= 1")!!.evaluate())
  }

  @Test
  fun `test greater number`() {
    assertEquals(VimInt.ONE, VimscriptParser.parseExpression("2 >= 1")!!.evaluate())
  }

  @Test
  fun `test smaller number`() {
    assertEquals(VimInt.ZERO, VimscriptParser.parseExpression("1 >= 2")!!.evaluate())
  }

  @Test
  fun `test equal strings`() {
    assertEquals(VimInt.ONE, VimscriptParser.parseExpression("'abc' >= 'abc'")!!.evaluate())
  }

  @Test
  fun `test greater string`() {
    assertEquals(VimInt.ONE, VimscriptParser.parseExpression("'def' >= 'abc'")!!.evaluate())
  }

  @Test
  fun `test smaller string`() {
    assertEquals(VimInt.ZERO, VimscriptParser.parseExpression("'abc' >= 'def'")!!.evaluate())
  }

  @Test
  fun `test compare String and Number compares as Number`() {
    // The String is parsed as 0
    assertEquals(VimInt.ZERO, VimscriptParser.parseExpression("'abc' >= 1")!!.evaluate())
    assertEquals(VimInt.ONE, VimscriptParser.parseExpression("1 >= 'two'")!!.evaluate())
    assertEquals(VimInt.ONE, VimscriptParser.parseExpression("2 >= '1'")!!.evaluate())
    assertEquals(VimInt.ZERO, VimscriptParser.parseExpression("2 >= '3'")!!.evaluate())
  }
}
