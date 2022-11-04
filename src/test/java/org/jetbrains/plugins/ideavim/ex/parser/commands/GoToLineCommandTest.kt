/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.commands

import com.maddyhome.idea.vim.vimscript.model.commands.GoToLineCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertTrue

class GoToLineCommandTest {

  @Test
  fun `digit as go to line command`() {
    val c = VimscriptParser.parseCommand("4")
    assertTrue(c is GoToLineCommand)
  }

  @Test
  fun `number as go to line command`() {
    val c = VimscriptParser.parseCommand("14")
    assertTrue(c is GoToLineCommand)
  }
}
