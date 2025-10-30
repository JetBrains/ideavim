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

class BitwiseRightShiftOperatorTest : VimTestCase() {
  @Test
  fun `test bitwise right shift with Float value reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2.3 >> 2")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise right shift with String value does not coerce and reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("'3' >> 2")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise right shift with Float bit count reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2 >> 2.34")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise right shift with String bit count does not coerce and reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2 >> '2'")!!.evaluate()
    }
    assertEquals("E1282: Bitshift operands must be numbers", exception.message)
  }

  @Test
  fun `test bitwise right shift with negative bit count reports error`() {
    val exception = assertThrows<ExException> {
      VimscriptParser.parseExpression("2 >> -2")!!.evaluate()
    }
    assertEquals("E1283: Bitshift amount must be a positive number", exception.message)
  }

  @Test
  fun `test bitwise right shift`() {
    assertEquals(VimInt(2), VimscriptParser.parseExpression("32 >> 4")!!.evaluate())
  }

  @Test
  fun `test bitwise right shift with 0 bit count`() {
    assertEquals(VimInt(2), VimscriptParser.parseExpression("2 >> 0")!!.evaluate())
  }

  @Test
  fun `test bitwise right shift with 1 bit count`() {
    assertEquals(VimInt(2), VimscriptParser.parseExpression("4 >> 1")!!.evaluate())
  }

  @Test
  fun `test bitwise right shift with negative value`() {
    // -4 is 0b1111_1111_1111_1111_1111_1111_1111_1100
    // Shifting one to the right gives 0b1111_1111_1111_1111_1111_1111_1111_1110 (preserve sign bit) => -2
    assertEquals(VimInt(-2), VimscriptParser.parseExpression("-4 >> 1")!!.evaluate())
  }

  @Test
  fun `test bitwise right shift with negative value 2`() {
    // -4 is 0b1111_1111_1111_1111_1111_1111_1111_1100
    // Shifting 8 to the right gives 0b1111_1111_1111_1111_1111_1111_1111_1111 (preserve sign bit and fill with 1s) => -1
    assertEquals(VimInt(-1), VimscriptParser.parseExpression("-4 >> 8")!!.evaluate())
  }

  @Test
  fun `test bitwise right shift with bit count equal to the max bits in Vim Number returns 0`() {
    // Vim has `v:numbersize` of 64, IdeaVim's VimInt is an Int, so has a max bit size of 32
    assertEquals(VimInt(1), VimscriptParser.parseExpression("2147483647 >> 30")!!.evaluate())
    assertEquals(VimInt(0), VimscriptParser.parseExpression("2147483647 >> 31")!!.evaluate())
    assertEquals(VimInt(0), VimscriptParser.parseExpression("2147483647 >> 32")!!.evaluate())
    assertEquals(VimInt(0), VimscriptParser.parseExpression("2147483647 >> 33")!!.evaluate())
  }

  @Test
  fun `test bitwise right shift with large number`() {
    val value = Int.MAX_VALUE // 2147483647
    // 2147483647 is 0b0111_1111_1111_1111_1111_1111_1111_1111
    // 0b0011_1111_1111_1111_1111_1111_1111_1111 is 1073741823
    assertEquals(VimInt(1073741823), VimscriptParser.parseExpression("$value >> 1")!!.evaluate())
  }

  @Test
  fun `test chained bitwise right shift operators`() {
    assertEquals(VimInt(2), VimscriptParser.parseExpression("128 >> 2 >> 4")!!.evaluate())
  }
}
