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

package org.jetbrains.plugins.ideavim.ex.parser.commands

import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandTests {

  @Test
  fun `let command`() {
    val c = VimscriptParser.parseCommand("let g:catSound='Meow'")
    assertTrue(c is LetCommand)
    assertEquals(Variable(Scope.GLOBAL_VARIABLE, "catSound"), c.variable)
    assertEquals(SimpleExpression(VimString("Meow")), c.expression)
  }

  @Test
  fun `echo command`() {
    val c = VimscriptParser.parseCommand("echo 4 5+7 'hi doggy'")
    assertTrue(c is EchoCommand)
    val expressions = c.args
    assertEquals(3, expressions.size)
    assertTrue(expressions[0] is SimpleExpression)
    assertEquals(VimInt(4), (expressions[0] as SimpleExpression).data)
    assertTrue(expressions[1] is BinExpression)
    assertEquals(VimInt(12), expressions[1].evaluate(null, null, VimContext()))
    assertTrue(expressions[2] is SimpleExpression)
    assertEquals(VimString("hi doggy"), (expressions[2] as SimpleExpression).data)
  }
}
