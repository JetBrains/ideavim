/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
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
    assertTrue(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
    typeText(commandToKeys("set rnu!"))
    assertFalse(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, OptionConstants.relativenumberName))
  }

  // todo we have spaces in assertExOutput because of pad(20) in the com.maddyhome.idea.vim.vimscript.model.commands.SetCommandKt#showOptions method
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test number option`() {
    configureByText("\n")
    typeText(commandToKeys("set scrolloff&"))
    assertEquals(VimInt(0), injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName))
    typeText(commandToKeys("set scrolloff?"))
    assertExOutput("scrolloff=0         \n")
    typeText(commandToKeys("set scrolloff=5"))
    assertEquals(VimInt(5), injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName))
    typeText(commandToKeys("set scrolloff?"))
    assertExOutput("scrolloff=5         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test toggle option as a number`() {
    configureByText("\n")
    typeText(commandToKeys("set number&"))
    assertEquals(VimInt(0), injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.numberName))
    typeText(commandToKeys("set number?"))
    assertExOutput("nonumber            \n")
    typeText(commandToKeys("let &nu=1000"))
    assertEquals(VimInt(1000), injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.numberName))
    typeText(commandToKeys("set number?"))
    assertExOutput("  number            \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test toggle option exceptions`() {
    configureByText("\n")
    typeText(commandToKeys("set number+=10"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=10")
    typeText(commandToKeys("set number+=test"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=test")

    typeText(commandToKeys("set number^=10"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=10")
    typeText(commandToKeys("set number^=test"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=test")

    typeText(commandToKeys("set number-=10"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=10")
    typeText(commandToKeys("set number-=test"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=test")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test number option exceptions`() {
    configureByText("\n")
    typeText(commandToKeys("set scrolloff+=10"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff+=10")
    typeText(commandToKeys("set scrolloff+=test"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff+=test")

    typeText(commandToKeys("set scrolloff^=10"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff^=10")
    typeText(commandToKeys("set scrolloff^=test"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff^=test")

    typeText(commandToKeys("set scrolloff-=10"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff-=10")
    typeText(commandToKeys("set scrolloff-=test"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff-=test")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test string option`() {
    configureByText("\n")
    typeText(commandToKeys("set selection&"))
    assertEquals(VimString("inclusive"), injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.selectionName))
    typeText(commandToKeys("set selection?"))
    assertExOutput("selection=inclusive \n")
    typeText(commandToKeys("set selection=exclusive"))
    assertEquals(VimString("exclusive"), injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.selectionName))
    typeText(commandToKeys("set selection?"))
    assertExOutput("selection=exclusive \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test show numbered value`() {
    configureByText("\n")
    typeText(commandToKeys("set so"))
    assertExOutput("scrolloff=0         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test show numbered value with questionmark`() {
    configureByText("\n")
    typeText(commandToKeys("set so?"))
    assertExOutput("scrolloff=0         \n")
  }
}
