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

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.services.OptionService
import org.jetbrains.plugins.ideavim.VimTestCase

class SetCommandTest : VimTestCase() {

  fun `test unknown option`() {
    configureByText("\n")
    typeText(commandToKeys("set unknownOption"))
    assertPluginError(true)
    assertPluginErrorMessageContains("Unknown option: unknownOption")
  }

  fun `test toggle option`() {
    configureByText("\n")
    typeText(commandToKeys("set rnu"))
    assertTrue(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu"))
    typeText(commandToKeys("set rnu!"))
    assertFalse(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "rnu"))
  }
}
