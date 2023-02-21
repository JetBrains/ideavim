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
    enterCommand("set unknownOption")
    assertPluginError(true)
    assertPluginErrorMessageContains("Unknown option: unknownOption")
  }

  fun `test toggle option`() {
    configureByText("\n")
    enterCommand("set rnu")
    assertTrue(options().isSet(OptionConstants.relativenumber))
    enterCommand("set rnu!")
    assertFalse(options().isSet(OptionConstants.relativenumber))
  }

  // todo we have spaces in assertExOutput because of pad(20) in the com.maddyhome.idea.vim.vimscript.model.commands.SetCommandKt#showOptions method
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test number option`() {
    configureByText("\n")
    enterCommand("set scrolloff&")
    assertEquals(0, options().getIntValue(OptionConstants.scrolloff))
    enterCommand("set scrolloff?")
    assertExOutput("scrolloff=0         \n")
    enterCommand("set scrolloff=5")
    assertEquals(5, options().getIntValue(OptionConstants.scrolloff))
    enterCommand("set scrolloff?")
    assertExOutput("scrolloff=5         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test toggle option as a number`() {
    configureByText("\n")
    enterCommand("set number&")
    assertEquals(0, options().getIntValue(OptionConstants.number))
    enterCommand("set number?")
    assertExOutput("nonumber            \n")
    enterCommand("let &nu=1000")
    assertEquals(1000, options().getIntValue(OptionConstants.number))
    enterCommand("set number?")
    assertExOutput("  number            \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test toggle option exceptions`() {
    configureByText("\n")
    enterCommand("set number+=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=10")
    enterCommand("set number+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=test")

    enterCommand("set number^=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=10")
    enterCommand("set number^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=test")

    enterCommand("set number-=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=10")
    enterCommand("set number-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=test")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test number option exceptions`() {
    configureByText("\n")
    enterCommand("set scrolloff+=10")
    assertPluginError(false)
    enterCommand("set scrolloff+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff+=test")

    enterCommand("set scrolloff^=10")
    assertPluginError(false)
    enterCommand("set scrolloff^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff^=test")

    enterCommand("set scrolloff-=10")
    assertPluginError(false)
    enterCommand("set scrolloff-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff-=test")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test string option`() {
    configureByText("\n")
    enterCommand("set selection&")
    assertEquals("inclusive", options().getStringValue(OptionConstants.selection))
    enterCommand("set selection?")
    assertExOutput("selection=inclusive \n")
    enterCommand("set selection=exclusive")
    assertEquals("exclusive", options().getStringValue(OptionConstants.selection))
    enterCommand("set selection?")
    assertExOutput("selection=exclusive \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test show numbered value`() {
    configureByText("\n")
    enterCommand("set so")
    assertExOutput("scrolloff=0         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test show numbered value with questionmark`() {
    configureByText("\n")
    enterCommand("set so?")
    assertExOutput("scrolloff=0         \n")
  }
}
