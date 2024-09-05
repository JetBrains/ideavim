/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.DigraphCommand
import org.jetbrains.plugins.ideavim.VimTestCaseBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DigraphsCommandTest : VimTestCaseBase() {
  @Test
  fun `test digraphs is parsed correctly`() {
    val exCommand = injector.vimscriptParser.parseCommand("digraphs")
    assertTrue(exCommand is DigraphCommand)
  }
}