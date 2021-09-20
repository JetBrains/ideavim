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
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.FromDataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(Theories::class)
class ConcatenationOperatorTest {

  companion object {
    @JvmStatic
    val operator = listOf(".", "..")
      @DataPoints("operator") get
    @JvmStatic
    val spaces = listOf("", " ")
      @DataPoints("spaces") get
  }

  @Theory
  fun `integer and integer`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp: String) {
    assertEquals(VimString("23"), VimscriptParser.parseExpression("2$sp$operator 3")!!.evaluate())
    assertEquals(VimString("23"), VimscriptParser.parseExpression("2 $operator${sp}3")!!.evaluate())
  }

  @Theory
  fun `integer and float`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("3.4$sp1$operator${sp2}2")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E806: using Float as a String", e.message)
    }
  }

  @Theory
  fun `float and float`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("3.4$sp1$operator${sp2}2.2")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E806: using Float as a String", e.message)
    }
  }

  @Theory
  fun `string and float`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("'string'$sp1$operator${sp2}3.4")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E806: using Float as a String", e.message)
    }
  }

  @Theory
  fun `string and string`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    assertEquals(
      VimString("stringtext"),
      VimscriptParser.parseExpression("'string'$sp1$operator$sp2'text'")!!.evaluate()
    )
  }

  @Theory
  fun `string and integer`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    assertEquals(
      VimString("string3"),
      VimscriptParser.parseExpression("'string'$sp1$operator${sp2}3")!!.evaluate()
    )
  }

  @Theory
  fun `String and list`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("2$sp1$operator$sp2[1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @Theory
  fun `string and list`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("'string'$sp1$operator$sp2[1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @Theory
  fun `list and list`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("[3]$sp1$operator$sp2[1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @Theory
  fun `dict and integer`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator${sp2}1")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Theory
  fun `dict and float`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator${sp2}1.4")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Theory
  fun `dict and string`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator$sp2'string'")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Theory
  fun `dict and list`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator$sp2[1]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @Theory
  fun `dict and dict`(@FromDataPoints("operator") operator: String, @FromDataPoints("spaces") sp1: String, @FromDataPoints("spaces") sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator$sp2{'key2': 33}")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }
}
