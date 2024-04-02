/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VimLContextTest {

  @Test
  fun `get first context test`() {
    val echoCommand = EchoCommand(Range(), listOf(SimpleExpression("oh, hi Mark")))
    val ifStatement1 = IfStatement(listOf(Pair(SimpleExpression(1), listOf(echoCommand))))
    val ifStatement2 = IfStatement(listOf(Pair(SimpleExpression(1), listOf(ifStatement1))))
    val script = Script(listOf(ifStatement2))

    echoCommand.vimContext = ifStatement1
    ifStatement1.vimContext = ifStatement2
    ifStatement2.vimContext = script

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
