/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SubtractionOperatorTest : VimTestCase() {

  @Test
  fun `integer minus integer`() {
    assertEquals(VimInt(-1), VimscriptParser.parseExpression("2 - 3")!!.evaluate())
  }

  @Test
  fun `integer minus float`() {
    assertEquals(VimFloat(-1.4), VimscriptParser.parseExpression("2 - 3.4")!!.evaluate())
  }

  @Test
  fun `float minus float`() {
    assertEquals(VimFloat(1.25), VimscriptParser.parseExpression("4.25 - 3.0")!!.evaluate())
  }

  @Test
  fun `string minus float`() {
    assertEquals(VimFloat(-3.4), VimscriptParser.parseExpression("'string' - 3.4")!!.evaluate())
  }

  @Test
  fun `string minus string`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("'string' - 'text'")!!.evaluate())
  }

  @Test
  fun `string minus integer`() {
    assertEquals(VimInt(-3), VimscriptParser.parseExpression("'string' - 3")!!.evaluate())
  }

  @Test
  fun `number minus list`() {
    try {
      VimscriptParser.parseExpression("2 - [1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `string minus list`() {
    try {
      VimscriptParser.parseExpression("'string' - [1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `list minus list`() {
    try {
      VimscriptParser.parseExpression("[3] - [1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `dict minus integer`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - 1")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus float`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - 1.4")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus string`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - 'string'")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus list`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - [1]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus dict`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - {'key2': 33}")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }
}
