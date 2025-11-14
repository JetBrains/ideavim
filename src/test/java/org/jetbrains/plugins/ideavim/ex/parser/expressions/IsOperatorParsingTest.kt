/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.assertEquals

class IsOperatorParsingTest : VimTestCase() {
  @Test
  fun `test can use is as variable name`() {
    val command = VimscriptParser.parseCommand("let is=12")
    assertInstanceOf<LetCommand>(command)
    val variable = assertInstanceOf<VariableExpression>(command.lvalue)
    assertEquals(VimString("is"), variable.name.evaluate())
  }

  @Test
  fun `test can use isnot as variable name`() {
    val command = VimscriptParser.parseCommand("let isnot=12")
    assertInstanceOf<LetCommand>(command)
    val variable = assertInstanceOf<VariableExpression>(command.lvalue)
    assertEquals(VimString("isnot"), variable.name.evaluate())
  }

  @Test
  fun `test can use is as variable name in expression`() {
    val command = VimscriptParser.parseCommand("echo is")
    val echo = assertInstanceOf<EchoCommand>(command)
    val expr = assertInstanceOf<VariableExpression>(echo.args[0])
    assertEquals(VimString("is"), expr.name.evaluate())
  }

  @Test
  fun `test can use isnot as variable name in expression`() {
    val command = VimscriptParser.parseCommand("echo isnot")
    val echo = assertInstanceOf<EchoCommand>(command)
    val expr = assertInstanceOf<VariableExpression>(echo.args[0])
    assertEquals(VimString("isnot"), expr.name.evaluate())
  }

  @Test
  fun `test can use isnot as operator in expression`() {
    val expr = VimscriptParser.parseExpression("12 isnot 12")
    assertInstanceOf<BinExpression>(expr)
    assertEquals(BinaryOperator.IS_NOT, expr.operator)
  }
}
