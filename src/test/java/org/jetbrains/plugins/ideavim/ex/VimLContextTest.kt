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

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VimLContextTest {

  @Test
  fun `get first context test`() {
    val echoCommand = EchoCommand(Ranges(), listOf(SimpleExpression("oh, hi Mark")))
    val ifStatement1 = IfStatement(listOf(Pair(SimpleExpression(1), listOf(echoCommand))))
    val ifStatement2 = IfStatement(listOf(Pair(SimpleExpression(1), listOf(ifStatement1))))
    val script = Script(listOf(ifStatement2))

    echoCommand.parent = ifStatement1
    ifStatement1.parent = ifStatement2
    ifStatement2.parent = script

    assertEquals(script, echoCommand.getFirstParentContext())
  }

  @Test
  fun `script is a first parent`() {
    assertTrue(Script().isFirstParentContext())
  }

  @Test
  fun `command line is a first parent`() {
    assertTrue(CommandLineVimLContext.isFirstParentContext())
  }
}
