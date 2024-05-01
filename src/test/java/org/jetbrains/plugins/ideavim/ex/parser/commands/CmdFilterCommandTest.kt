/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.commands

import com.maddyhome.idea.vim.vimscript.model.commands.CmdFilterCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CmdFilterCommandTest : VimTestCase() {

  // VIM-2442
  @Test
  fun `test quotes inside single quotes`() {
    val cmd = VimscriptParser.parseCommand("!echo '\"test\"'")
    assertTrue(cmd is CmdFilterCommand)
    assertEquals("echo '\"test\"'", cmd.argument)
  }
}
