/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NumberParsingTests : VimTestCase() {

  @Test
  fun `one digit decimal number`() {
    assertEquals(VimInt(4), VimscriptParser.parseExpression("4")!!.evaluate())
  }

  @Test
  fun `decimal number`() {
    assertEquals(VimInt(12), VimscriptParser.parseExpression("12")!!.evaluate())
  }

  @Test
  fun `negative decimal number`() {
    assertEquals(VimInt(-10), VimscriptParser.parseExpression("-10")!!.evaluate())
  }

  @Test
  fun `hex number`() {
    assertEquals(VimInt(256), VimscriptParser.parseExpression("0x100")!!.evaluate())
  }

  @Test
  fun `negative hex number`() {
    assertEquals(VimInt(-16), VimscriptParser.parseExpression("-0x10")!!.evaluate())
  }

  @Test
  fun `upper and lower case hex number`() {
    assertEquals(VimInt(171), VimscriptParser.parseExpression("0XaB")!!.evaluate())
  }

  @Test
  fun `decimal number with leading zero`() {
    assertEquals(VimInt(19), VimscriptParser.parseExpression("019")!!.evaluate())
  }

  @Test
  fun `decimal number with multiple leading zeros`() {
    assertEquals(VimInt(19), VimscriptParser.parseExpression("00019")!!.evaluate())
  }

  @Test
  fun `one digit octal number`() {
    assertEquals(VimInt(7), VimscriptParser.parseExpression("07")!!.evaluate())
  }

  @Test
  fun `octal number`() {
    assertEquals(VimInt(15), VimscriptParser.parseExpression("017")!!.evaluate())
  }

  @Test
  fun `octal number with multiple leading zeros`() {
    assertEquals(VimInt(15), VimscriptParser.parseExpression("00017")!!.evaluate())
  }

  @Test
  fun `negative octal number`() {
    assertEquals(VimInt(-24), VimscriptParser.parseExpression("-030")!!.evaluate())
  }

  @Test
  fun `octal number with lowercase prefix`() {
    assertEquals(VimInt(15), VimscriptParser.parseExpression("0o17")!!.evaluate())
  }

  @Test
  fun `octal number with uppercase prefix`() {
    assertEquals(VimInt(15), VimscriptParser.parseExpression("0O17")!!.evaluate())
  }

  @Test
  fun `binary number with lowercase prefix`() {
    assertEquals(VimInt(15), VimscriptParser.parseExpression("0b1111")!!.evaluate())
  }

  @Test
  fun `binary number with uppercase prefix`() {
    assertEquals(VimInt(15), VimscriptParser.parseExpression("0B1111")!!.evaluate())
  }

  @Test
  fun `float number`() {
    assertEquals(VimFloat(4.0), VimscriptParser.parseExpression("4.0")!!.evaluate())
  }

  @Test
  fun `float number in scientific notation`() {
    assertEquals(VimFloat(4.0), VimscriptParser.parseExpression("0.4e+1")!!.evaluate())
  }

  @Test
  fun `float number in scientific notation with + omitted`() {
    assertEquals(VimFloat(4.0), VimscriptParser.parseExpression("0.4e1")!!.evaluate())
  }

  @Test
  fun `float number in scientific notation with negative exponent`() {
    assertEquals(VimFloat(0.48), VimscriptParser.parseExpression("4.8e-1")!!.evaluate())
  }

  @Test
  fun `negative float number`() {
    assertEquals(VimFloat(-12.1), VimscriptParser.parseExpression("-12.1")!!.evaluate())
  }

  @Test
  fun `negative float number in scientific notation`() {
    assertEquals(VimFloat(-124.56), VimscriptParser.parseExpression("-12.456e1")!!.evaluate())
  }
}
