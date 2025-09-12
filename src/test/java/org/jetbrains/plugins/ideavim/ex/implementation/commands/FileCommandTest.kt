/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.FileCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class FileCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("file")
    assertTrue(command is FileCommand)
  }

  @Test
  fun `test file outputs file details as message`() {
    configureByText("lorem ipsum")
    enterCommand("file")
    assertContains("\"/src/aaa.txt\" line 1 of 1 --0%-- col 1", VimPlugin.getMessage())
  }

  @Test
  fun `test file with range reports errors`() {
    configureByText("lorem ipsum")
    enterCommand("3file")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument")
  }

  @Test
  fun `test file with count reports errors`() {
    configureByText("lorem ipsum")
    // Technically, this would rename the current file to "3"
    enterCommand("file 3")
    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: 3")
  }
}
