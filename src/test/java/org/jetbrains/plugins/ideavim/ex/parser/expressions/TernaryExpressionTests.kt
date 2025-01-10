/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.TernaryExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TernaryExpressionTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ")

    @JvmStatic
    fun arg8(): List<Arguments> = productForArguments(values, values, values, values, values, values, values, values)
  }

  @ParameterizedTest
  @MethodSource("arg8")
  fun `simple expression`(
    sp1: String,
    sp2: String,
    sp3: String,
    sp4: String,
    sp5: String,
    sp6: String,
    sp7: String,
    sp8: String,
  ) {
    val expression = VimscriptParser.parseExpression("1$sp1?$sp2'2'$sp3:3")
    assertTrue(expression is TernaryExpression)
    assertEquals(SimpleExpression(1), expression.condition)
    assertEquals(SimpleExpression("2"), expression.then)
    assertEquals(SimpleExpression(3), expression.otherwise)
  }
}
