/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Elliot Courant
 */
class DelCmdHandlerTest : VimTestCase() {
  fun `test remove alias`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command"))
    assertPluginError(false)
    assertExOutput("Name        Args       Definition\n") // There should not be any aliases.

    typeText(commandToKeys("command Vs vs"))
    assertPluginError(false)
    typeText(commandToKeys("command Wq wq"))
    assertPluginError(false)
    typeText(commandToKeys("command WQ wq"))
    assertPluginError(false)
    typeText(commandToKeys("command"))
    assertPluginError(false)
    // The added alias should be listed
    assertExOutput("""Name        Args       Definition
            |Vs          0          vs
            |Wq          0          wq
            |WQ          0          wq
        """.trimMargin())

    typeText(commandToKeys("command W"))
    assertPluginError(false)
    // The filtered aliases should be listed
    assertExOutput("""Name        Args       Definition
            |Wq          0          wq
            |WQ          0          wq
        """.trimMargin())

    // Delete one of the aliases and then list all aliases again.
    typeText(commandToKeys("delcommand Wq"))
    assertPluginError(false)
    typeText(commandToKeys("command"))
    assertPluginError(false)
    assertExOutput("""Name        Args       Definition
            |Vs          0          vs
            |WQ          0          wq
        """.trimMargin())
  }

  fun `test remove non-existant alias`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("delcommand VS"))
    assertPluginError(true)
  }
}
