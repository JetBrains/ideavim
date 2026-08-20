/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.CmdFilterCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CmdFilterCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("!ls")
    assertTrue(command is CmdFilterCommand)
    assertEquals("ls", command.argument)
  }

  @Test
  @TestWithoutNeovim(
    reason = SkipNeovimReason.SEE_DESCRIPTION,
    description = "Runs an external command. Neovim's own output for `:!` is not routed through IdeaVim's ex output " +
      "panel, so there is nothing to compare.",
  )
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses the POSIX 'sort' command and a POSIX shell")
  fun `command reading stdin does not wait for input`() {
    configureByText("Lorem ipsum dolor sit amet\n")

    // `sort` with no arguments reads stdin. Vim hands such a command the terminal it is running in, and Neovim
    // redirects stdin to /dev/null. We have no terminal to hand over, so the command must see an immediate EOF and
    // exit, rather than blocking forever waiting for input that can never arrive.
    assertCommandOutput("!sort", ":!sort\n")
  }
}
