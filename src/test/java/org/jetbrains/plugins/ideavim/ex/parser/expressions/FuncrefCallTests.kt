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

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OneElementSublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuncrefCallTests {

  @Test
  fun `test dictionary funcref call`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.len()")
    assertTrue(funcrefCall is FuncrefCallExpression)
    assertEquals(
      OneElementSublistExpression(SimpleExpression(VimString("len")), Variable(null, "dict")),
      funcrefCall.expression
    )
    assertEquals(0, funcrefCall.args.size)
  }

  @Test
  fun `test inner dictionary funcref call`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.innerDict.len()")
    assertTrue(funcrefCall is FuncrefCallExpression)
    assertEquals(
      OneElementSublistExpression(SimpleExpression(VimString("len")), OneElementSublistExpression(SimpleExpression(VimString("innerDict")), Variable(null, "dict"))),
      funcrefCall.expression
    )
    assertEquals(0, funcrefCall.args.size)
  }

  @Test
  fun `test dictionary funcref call with args`() {
    val funcrefCall = VimscriptParser.parseExpression("dict.len(a, 5 + 10)")
    assertTrue(funcrefCall is FuncrefCallExpression)
    assertEquals(
      OneElementSublistExpression(SimpleExpression(VimString("len")), Variable(null, "dict")),
      funcrefCall.expression
    )
    assertEquals(2, funcrefCall.args.size)
    assertEquals(Variable(null, "a"), funcrefCall.args[0])
    assertEquals(BinExpression(SimpleExpression(VimInt(5)), SimpleExpression(VimInt(10)), BinaryOperator.ADDITION), funcrefCall.args[1])
  }
}
