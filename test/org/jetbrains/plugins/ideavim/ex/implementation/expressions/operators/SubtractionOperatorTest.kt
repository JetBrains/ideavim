/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.Test
import kotlin.test.assertEquals

class SubtractionOperatorTest {

  @Test
  fun `integer minus integer`() {
    assertEquals(VimInt(-1), VimscriptParser.parseExpression("2 - 3").evaluate())
  }

  @Test
  fun `integer minus float`() {
    assertEquals(VimFloat(-1.4), VimscriptParser.parseExpression("2 - 3.4").evaluate())
  }

  @Test
  fun `float minus float`() {
    assertEquals(VimFloat(1.25), VimscriptParser.parseExpression("4.25 - 3.0").evaluate())
  }

  @Test
  fun `string minus float`() {
    assertEquals(VimFloat(-3.4), VimscriptParser.parseExpression("'string' - 3.4").evaluate())
  }

  @Test
  fun `string minus string`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("'string' - 'text'").evaluate())
  }

  @Test
  fun `string minus integer`() {
    assertEquals(VimInt(-3), VimscriptParser.parseExpression("'string' - 3").evaluate())
  }

  @Test
  fun `number minus list`() {
    try {
      VimscriptParser.parseExpression("2 - [1, 2]").evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `string minus list`() {
    try {
      VimscriptParser.parseExpression("'string' - [1, 2]").evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `list minus list`() {
    try {
      VimscriptParser.parseExpression("[3] - [1, 2]").evaluate()
    } catch (e: ExException) {
      assertEquals("E745: Using a List as a Number", e.message)
    }
  }

  @Test
  fun `dict minus integer`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - 1").evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus float`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - 1.4").evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus string`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - 'string'").evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus list`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - [1]").evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }

  @Test
  fun `dict minus dict`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} - {'key2': 33}").evaluate()
    } catch (e: ExException) {
      assertEquals("E728: Using a Dictionary as a Number", e.message)
    }
  }
}
