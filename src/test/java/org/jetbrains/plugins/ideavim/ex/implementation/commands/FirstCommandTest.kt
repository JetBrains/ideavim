/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.SelectFirstFileCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class FirstCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("first")
    assertTrue(command is SelectFirstFileCommand)
  }

  @Test
  fun `command parsing bfirst`() {
    val command = injector.vimscriptParser.parseCommand("bfirst")
    assertTrue(command is SelectFirstFileCommand)
  }

  @Test
  fun `command parsing bf`() {
    val command = injector.vimscriptParser.parseCommand("bf")
    assertTrue(command is SelectFirstFileCommand)
  }

  @Test
  fun `command parsing brewind`() {
    val command = injector.vimscriptParser.parseCommand("brewind")
    assertTrue(command is SelectFirstFileCommand)
  }

  @Test
  fun `command parsing br`() {
    val command = injector.vimscriptParser.parseCommand("br")
    assertTrue(command is SelectFirstFileCommand)
  }
}