/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.EditFileCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditFileCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("edit ~/.ideavimrc")
    assertTrue(command is EditFileCommand)
    assertEquals("~/.ideavimrc", command.argument)
  }

  @Test
  fun `command parsing 2`() {
    val command = injector.vimscriptParser.parseCommand("browse ~/.ideavimrc")
    assertTrue(command is EditFileCommand)
    assertEquals("~/.ideavimrc", command.argument)
  }
}