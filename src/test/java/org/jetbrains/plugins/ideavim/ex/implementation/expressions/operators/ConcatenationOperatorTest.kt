/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class ConcatenationOperatorTest : VimTestCase() {

  companion object {
    @JvmStatic
    val operator = listOf(".", "..")

    @JvmStatic
    val spaces = listOf("", " ")

    @JvmStatic
    fun operatorSpaces(): List<Arguments> = productForArguments(operator, spaces)

    @JvmStatic
    fun operatorSpacesSpaces(): List<Arguments> = productForArguments(operator, spaces, spaces)
  }

  @ParameterizedTest
  @MethodSource("operatorSpaces")
  fun `integer and integer`(operator: String, sp: String) {
    assertEquals(VimString("23"), VimscriptParser.parseExpression("2$sp$operator 3")!!.evaluate())
    assertEquals(VimString("23"), VimscriptParser.parseExpression("2 $operator${sp}3")!!.evaluate())
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `integer and float`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("3.4$sp1$operator${sp2}2")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E806: Using a Float as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `float and float`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("3.4$sp1$operator${sp2}2.2")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E806: Using a Float as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `string and float`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("'string'$sp1$operator${sp2}3.4")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E806: Using a Float as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `string and string`(operator: String, sp1: String, sp2: String) {
    assertEquals(
      VimString("stringtext"),
      VimscriptParser.parseExpression("'string'$sp1$operator$sp2'text'")!!.evaluate(),
    )
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `string and integer`(operator: String, sp1: String, sp2: String) {
    assertEquals(
      VimString("string3"),
      VimscriptParser.parseExpression("'string'$sp1$operator${sp2}3")!!.evaluate(),
    )
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `String and list`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("2$sp1$operator$sp2[1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `string and list`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("'string'$sp1$operator$sp2[1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `list and list`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("[3]$sp1$operator$sp2[1, 2]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E730: Using a List as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `dict and integer`(operator: String, sp1: String, sp2: String) {
    try {
      if (sp1 == "" && sp2 == "") { // it is not a concatenation, so let's skip this case
        throw ExException("E731: Using a Dictionary as a String")
      }
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator${sp2}1")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `dict and float`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator${sp2}1.4")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `dict and string`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator$sp2'string'")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `dict and list`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator$sp2[1]")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }

  @ParameterizedTest
  @MethodSource("operatorSpacesSpaces")
  fun `dict and dict`(operator: String, sp1: String, sp2: String) {
    try {
      VimscriptParser.parseExpression("{'key' : 21}$sp1$operator$sp2{'key2': 33}")!!.evaluate()
    } catch (e: ExException) {
      assertEquals("E731: Using a Dictionary as a String", e.message)
    }
  }
}
