/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SublistExpressionTests {

  companion object {
    @JvmStatic
    fun args2(): List<Arguments> = productForArguments(spaces, spaces)

    @JvmStatic
    fun args3(): List<Arguments> = productForArguments(spaces, spaces, spaces)

    @JvmStatic
    fun args4(): List<Arguments> = productForArguments(spaces, spaces, spaces, spaces)

    @JvmStatic
    fun args6(): List<Arguments> = productForArguments(spaces, spaces, spaces, spaces, spaces, spaces)

    val spaces = listOf("", " ")
  }

  @ParameterizedTest
  @MethodSource("args2")
  fun `sublist with no range specified`(sp1: String, sp2: String) {
    val ex = VimscriptParser.parseExpression("1[$sp1:$sp2]")
    assertTrue(ex is SublistExpression)
    assertEquals(VimInt(1), ex.expression.evaluate())
    assertNull(ex.from)
    assertNull(ex.to)
  }

  @ParameterizedTest
  @MethodSource("args3")
  fun `sublist with only start specified`(sp1: String, sp2: String, sp3: String) {
    val ex = VimscriptParser.parseExpression("'text'[${sp1}2$sp2:$sp3]")
    assertTrue(ex is SublistExpression)
    assertEquals(VimString("text"), ex.expression.evaluate())
    assertEquals(VimInt(2), ex.from!!.evaluate())
    assertNull(ex.to)
  }

  @ParameterizedTest
  @MethodSource("args3")
  fun `sublist with only end specified`(sp1: String, sp2: String, sp3: String) {
    val ex = VimscriptParser.parseExpression("var[$sp1:${sp2}32$sp3]")
    assertTrue(ex is SublistExpression)
    assertTrue(ex.expression is Variable)
    assertEquals("var", (ex.expression as Variable).name.evaluate().asString())
    assertNull(ex.from)
    assertEquals(VimInt(32), ex.to!!.evaluate())
  }

  @ParameterizedTest
  @MethodSource("args4")
  fun `sublist with range specified`(sp1: String, sp2: String, sp3: String, sp4: String) {
    val ex = VimscriptParser.parseExpression("[1, 2, 3, 4, 5, 6][${sp1}1$sp2:${sp3}4$sp4]")
    assertTrue(ex is SublistExpression)
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3), VimInt(4), VimInt(5), VimInt(6))),
      ex.expression.evaluate(),
    )
    assertEquals(VimInt(1), ex.from!!.evaluate())
    assertEquals(VimInt(4), ex.to!!.evaluate())
  }

  @ParameterizedTest
  @MethodSource("args6")
  fun `sublist with non int expressions in ranges`(
    sp1: String,
    sp2: String,
    sp3: String,
    sp4: String,
    sp5: String,
    sp6: String,
  ) {
    val ex = VimscriptParser.parseExpression("[1, 2, 3, 4, 5, 6][${sp1}1$sp2+${sp3}5$sp4:$sp5'asd'$sp6]")
    assertTrue(ex is SublistExpression)
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3), VimInt(4), VimInt(5), VimInt(6))),
      ex.expression.evaluate(),
    )
    assertEquals(VimInt(6), ex.from!!.evaluate())
    assertEquals(VimString("asd"), ex.to!!.evaluate())
  }
}
