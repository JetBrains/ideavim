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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.Test
import kotlin.test.assertEquals

class ConcatenationOperatorTest {

  @Test
  fun `integer and integer`() {
    assertEquals(VimString("23"), VimscriptParser.parseExpression("2 . 3").evaluate())
  }

  @Test
  fun `integer and float`() {
    try {
      VimscriptParser.parseExpression("3.4 . 2").evaluate()
    } catch (e: ExException) {
      assertEquals("E806: using Float as a String", e.message)
    }
  }

  @Test
  fun `float and float`() {
    try {
      VimscriptParser.parseExpression("3.4 . 2.2").evaluate()
    } catch (e: ExException) {
      assertEquals("E806: using Float as a String", e.message)
    }
  }

  @Test
  fun `string and float`() {
    try {
      VimscriptParser.parseExpression("'string' . 3.4").evaluate()
    } catch (e: ExException) {
      assertEquals("E806: using Float as a String", e.message)
    }
  }

  @Test
  fun `string and string`() {
    assertEquals(
      VimString("stringtext"),
      VimscriptParser.parseExpression("'string' . 'text'").evaluate()
    )
  }

  @Test
  fun `string and integer`() {
    assertEquals(
      VimString("string3"),
      VimscriptParser.parseExpression("'string' . 3").evaluate()
    )
  }

  @Test
  fun `String and list`() {
    try {
      VimscriptParser.parseExpression("2 . [1, 2]").evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @Test
  fun `string and list`() {
    try {
      VimscriptParser.parseExpression("'string' . [1, 2]").evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @Test
  fun `list and list`() {
    try {
      VimscriptParser.parseExpression("[3] . [1, 2]").evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @Test
  fun `dict and integer`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} . 1").evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Test
  fun `dict and float`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} . 1.4").evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Test
  fun `dict and string`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} . 'string'").evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Test
  fun `dict and list`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} . [1]").evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Test
  fun `dict and dict`() {
    try {
      VimscriptParser.parseExpression("{'key' : 21} . {'key2': 33}").evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }
}
