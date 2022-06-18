/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

class LetCommandTest {

  @Test
  fun `let with register is parsed correctly`() {
    val script = injector.vimscriptParser.parse("let @+=5")
    assertEquals(1, script.units.size)
    val command = script.units.first()
    assertTrue(command is LetCommand)
    command as LetCommand
    assertEquals(Register('+'), command.variable)
    assertEquals(AssignmentOperator.ASSIGNMENT, command.operator)
    assertEquals(SimpleExpression(5), command.expression)
  }

  @Test
  fun `let with register is parsed correctly 2`() {
    val script = injector.vimscriptParser.parse("let @--=42")
    assertEquals(1, script.units.size)
    val command = script.units.first()
    assertTrue(command is LetCommand)
    command as LetCommand
    assertEquals(Register('-'), command.variable)
    assertEquals(AssignmentOperator.SUBTRACTION, command.operator)
    assertEquals(SimpleExpression(42), command.expression)
  }
}
