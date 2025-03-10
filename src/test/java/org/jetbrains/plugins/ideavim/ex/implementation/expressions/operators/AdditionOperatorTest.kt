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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdditionOperatorTest : VimTestCase() {

  @Test
  fun `integer plus integer`() {
    assertEquals(VimInt(5), VimscriptParser.parseExpression("2 + 3")!!.evaluate())
  }

  @Test
  fun `integer plus float`() {
    assertEquals(VimFloat(5.4), VimscriptParser.parseExpression("2 + 3.4")!!.evaluate())
  }

  @Test
  fun `float plus float`() {
    assertEquals(VimFloat(5.6), VimscriptParser.parseExpression("2.2 + 3.4")!!.evaluate())
  }

  @Test
  fun `string plus float`() {
    assertEquals(VimFloat(3.4), VimscriptParser.parseExpression("'string' + 3.4")!!.evaluate())
  }

  @Test
  fun `string plus string`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("'string' + 'text'")!!.evaluate())
  }

  @Test
  fun `string plus integer`() {
    assertEquals(VimInt(3), VimscriptParser.parseExpression("'string' + 3")!!.evaluate())
  }

  @Test
  fun `number plus list`() {
    try {
      VimscriptParser.parseExpression("2 + [1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `string plus list`() {
    try {
      VimscriptParser.parseExpression("'string' + [1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `list plus list`() {
    assertEquals(
      VimList(mutableListOf(VimInt(3), VimInt(1), VimInt(2))),
      VimscriptParser.parseExpression("[3] + [1, 2]")!!.evaluate(),
    )
  }

  @Test
  fun `dict plus integer`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} + 1")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict plus float`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} + 1.4")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict plus string`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} + 'string'")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict plus list`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} + [1]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict plus dict`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} + {'key2': 33}")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }
}
