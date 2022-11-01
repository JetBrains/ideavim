/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.TernaryExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Theories::class)
class TernaryExpressionTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun `simple expression`(sp1: String, sp2: String, sp3: String, sp4: String, sp5: String, sp6: String, sp7: String, sp8: String) {
    val expression = VimscriptParser.parseExpression("1$sp1?$sp2'2'$sp3:3")
    assertTrue(expression is TernaryExpression)
    assertEquals(SimpleExpression(1), expression.condition)
    assertEquals(SimpleExpression("2"), expression.then)
    assertEquals(SimpleExpression(3), expression.otherwise)
  }
}
