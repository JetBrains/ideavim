/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.options.OptionConstants
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
    assertTrue(options().isSet(OptionConstants.relativenumber))
    typeText(commandToKeys("set rnu!"))
    assertFalse(options().isSet(OptionConstants.relativenumber))
  }

  // todo we have spaces in assertExOutput because of pad(20) in the com.maddyhome.idea.vim.vimscript.model.commands.SetCommandKt#showOptions method
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test number option`() {
    configureByText("\n")
    typeText(commandToKeys("set scrolloff&"))
    assertEquals(0, options().getIntValue(OptionConstants.scrolloff))
    typeText(commandToKeys("set scrolloff?"))
    assertExOutput("scrolloff=0         \n")
    typeText(commandToKeys("set scrolloff=5"))
    assertEquals(5, options().getIntValue(OptionConstants.scrolloff))
    typeText(commandToKeys("set scrolloff?"))
    assertExOutput("scrolloff=5         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test toggle option as a number`() {
    configureByText("\n")
    typeText(commandToKeys("set number&"))
    assertEquals(0, options().getIntValue(OptionConstants.number))
    typeText(commandToKeys("set number?"))
    assertExOutput("nonumber            \n")
    typeText(commandToKeys("let &nu=1000"))
    assertEquals(1000, options().getIntValue(OptionConstants.number))
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
    assertEquals("inclusive", options().getStringValue(OptionConstants.selection))
    typeText(commandToKeys("set selection?"))
    assertExOutput("selection=inclusive \n")
    typeText(commandToKeys("set selection=exclusive"))
    assertEquals("exclusive", options().getStringValue(OptionConstants.selection))
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
