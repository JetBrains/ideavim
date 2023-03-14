/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SetCommandTest : VimTestCase() {

  @Test
  fun `test unknown option`() {
    configureByText("\n")
    enterCommand("set unknownOption")
    assertPluginError(true)
    assertPluginErrorMessageContains("Unknown option: unknownOption")
  }

  @Test
  fun `test toggle option`() {
    configureByText("\n")
    enterCommand("set rnu")
    kotlin.test.assertTrue(options().isSet(Options.relativenumber))
    enterCommand("set rnu!")
    kotlin.test.assertFalse(options().isSet(Options.relativenumber))
  }

  // todo we have spaces in assertExOutput because of pad(20) in the com.maddyhome.idea.vim.vimscript.model.commands.SetCommandKt#showOptions method
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test number option`() {
    configureByText("\n")
    enterCommand("set scrolloff&")
    kotlin.test.assertEquals(0, options().getIntValue(Options.scrolloff))
    enterCommand("set scrolloff?")
    assertExOutput("scrolloff=0         \n")
    enterCommand("set scrolloff=5")
    kotlin.test.assertEquals(5, options().getIntValue(Options.scrolloff))
    enterCommand("set scrolloff?")
    assertExOutput("scrolloff=5         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test toggle option as a number`() {
    configureByText("\n")
    enterCommand("set number&")
    kotlin.test.assertEquals(0, injector.optionGroup.getOptionValue(Options.number, OptionScope.GLOBAL).asDouble().toInt())
    enterCommand("set number?")
    assertExOutput("nonumber            \n")
    enterCommand("let &nu=1000")
    kotlin.test.assertEquals(1000, injector.optionGroup.getOptionValue(Options.number, OptionScope.GLOBAL).asDouble().toInt())
    enterCommand("set number?")
    assertExOutput("  number            \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
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
  @Test
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
  @Test
  fun `test string option`() {
    configureByText("\n")
    enterCommand("set selection&")
    kotlin.test.assertEquals("inclusive", options().getStringValue(Options.selection))
    enterCommand("set selection?")
    assertExOutput("selection=inclusive \n")
    enterCommand("set selection=exclusive")
    kotlin.test.assertEquals("exclusive", options().getStringValue(Options.selection))
    enterCommand("set selection?")
    assertExOutput("selection=exclusive \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test show numbered value`() {
    configureByText("\n")
    enterCommand("set so")
    assertExOutput("scrolloff=0         \n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test show numbered value with question mark`() {
    configureByText("\n")
    enterCommand("set so?")
    assertExOutput("scrolloff=0         \n")
  }
}
