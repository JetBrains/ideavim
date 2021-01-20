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
class CmdClearHandlerTest : VimTestCase() {
  fun `test clear aliases`() {
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

    // Delete all of the aliases and then list aliases again.
    typeText(commandToKeys("comclear"))
    assertPluginError(false)
    typeText(commandToKeys("command"))
    assertPluginError(false)
    assertExOutput("Name        Args       Definition\n") // There should not be any aliases.
  }
}
