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
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuncrefCallTests {

  @Test
  fun `test dictionary funcref call`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.len()")
    assertTrue(funcrefCall is FuncrefCallExpression)
    assertEquals(
      IndexedExpression(CurlyBracesName(listOf(SimpleExpression("len"))), Variable(null, "dict")),
      funcrefCall.expression,
    )
    assertEquals(0, funcrefCall.args.size)
  }

  @Test
  fun `test inner dictionary funcref call`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.innerDict.len()")
    assertTrue(funcrefCall is FuncrefCallExpression)
    assertEquals(
      IndexedExpression(
        CurlyBracesName(listOf(SimpleExpression("len"))),
        IndexedExpression(SimpleExpression("innerDict"), Variable(null, "dict")),
      ),
      funcrefCall.expression,
    )
    assertEquals(0, funcrefCall.args.size)
  }

  @Test
  fun `test dictionary funcref call with args`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.len(a, 5 + 10)")
    assertTrue(funcrefCall is FuncrefCallExpression)
    assertEquals(
      IndexedExpression(CurlyBracesName(listOf(SimpleExpression("len"))), Variable(null, "dict")),
      funcrefCall.expression,
    )
    assertEquals(2, funcrefCall.args.size)
    assertEquals(Variable(null, "a"), funcrefCall.args[0])
    assertEquals(BinExpression(SimpleExpression(5), SimpleExpression(10), BinaryOperator.ADDITION), funcrefCall.args[1])
  }
}
