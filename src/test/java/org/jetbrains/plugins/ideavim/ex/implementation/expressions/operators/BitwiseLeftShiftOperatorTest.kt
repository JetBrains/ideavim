/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BitwiseLeftShiftOperatorTest : VimTestCase() {
  @Test
  fun `test bitwise left shift with Float value reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2.3 << 2")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise left shift with String value does not coerce and reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("'3' << 2")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise left shift with Float bit count reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2 << 2.34")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise left shift with String bit count does not coerce and reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2 << '2'")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise left shift with negative bit count reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2 << -2")!!.evaluate()
    }
    assertEquals("E1283: Bitshift amount must be a positive number", exception.message)
  }

  @Test
  fun `test bitwise left shift`() {
    assertEquals(VimInt(32), VimscriptParser.parseExpression("2 << 4")!!.evaluate())
  }

  @Test
  fun `test bitwise left shift with 0 bit count`() {
    assertEquals(VimInt(2), VimscriptParser.parseExpression("2 << 0")!!.evaluate())
  }

  @Test
  fun `test bitwise left shift with 1 bit count`() {
    assertEquals(VimInt(4), VimscriptParser.parseExpression("2 << 1")!!.evaluate())
  }

  @Test
  fun `test bitwise left shift with negative value`() {
    assertEquals(VimInt(-4), VimscriptParser.parseExpression("-2 << 1")!!.evaluate())
  }

  @Test
  fun `test bitwise left shift with bit count equal to the max bits in Vim Number returns 0`() {
    // Vim has `v:numbersize` of 64, IdeaVim's VimInt is an Int, so has a max bit size of 32
    // E.g. in Vim: `echo 1<<64` => 0, but `echo 1<<63` returns -long.max_value
    assertEquals(VimInt(Int.MIN_VALUE), VimscriptParser.parseExpression("1 << 31")!!.evaluate())
    assertEquals(VimInt(0), VimscriptParser.parseExpression("1 << 32")!!.evaluate())
    assertEquals(VimInt(0), VimscriptParser.parseExpression("1 << 33")!!.evaluate())
  }

  @Test
  fun `test bitwise left shift with overflow`() {
    // Expression ends up negative, with the least significant bit set to 0, which == -2
    val value = 0b0111_1111_1111_1111_1111_1111_1111_1111
    assertEquals(VimInt(-2), VimscriptParser.parseExpression("$value << 1")!!.evaluate())
  }

  @Test
  fun `test bitwise left shift with overflow 2`() {
    val value = 0b0100_0000_0000_0000_0000_0000_0000_0000
    assertEquals(VimInt(Int.MIN_VALUE), VimscriptParser.parseExpression("$value << 1")!!.evaluate())
  }

  @Test
  fun `test chained bitwise left shift operators`() {
    assertEquals(VimInt(128), VimscriptParser.parseExpression("2 << 4 << 2")!!.evaluate())
  }
}
