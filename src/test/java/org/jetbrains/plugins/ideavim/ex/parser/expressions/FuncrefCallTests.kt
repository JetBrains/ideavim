/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.CurlyBracesName
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.IndexedExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.assertEquals

class FuncrefCallTests : VimTestCase() {

  @Test
  fun `test dictionary funcref call`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.len()")
    assertInstanceOf<FuncrefCallExpression>(funcrefCall)
    assertEquals(
      IndexedExpression(CurlyBracesName(listOf(SimpleExpression("len"))), VariableExpression(null, "dict")),
      funcrefCall.expression,
    )
    assertEquals(0, funcrefCall.args.size)
  }

  @Test
  fun `test inner dictionary funcref call`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.innerDict.len()")
    assertInstanceOf<FuncrefCallExpression>(funcrefCall)
    assertEquals(
      IndexedExpression(
        CurlyBracesName(listOf(SimpleExpression("len"))),
        IndexedExpression(SimpleExpression("innerDict"), VariableExpression(null, "dict")),
      ),
      funcrefCall.expression,
    )
    assertEquals(0, funcrefCall.args.size)
  }

  @Test
  fun `test dictionary funcref call with args`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.len(a, 5 + 10)")
    assertInstanceOf<FuncrefCallExpression>(funcrefCall)
    assertEquals(
      IndexedExpression(CurlyBracesName(listOf(SimpleExpression("len"))), VariableExpression(null, "dict")),
      funcrefCall.expression,
    )
    assertEquals(2, funcrefCall.args.size)
    assertEquals(VariableExpression(null, "a"), funcrefCall.args[0])
    assertEquals(BinExpression(SimpleExpression(5), SimpleExpression(10), BinaryOperator.ADDITION), funcrefCall.args[1])
  }
}
