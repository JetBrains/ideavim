/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.commands

import com.maddyhome.idea.vim.vimscript.model.commands.GoToLineCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class GoToLineCommandParserTest : VimTestCase() {

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
