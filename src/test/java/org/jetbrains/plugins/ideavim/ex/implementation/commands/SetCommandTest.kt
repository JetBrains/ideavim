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
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
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
    assertTrue(options().relativenumber)
    enterCommand("set rnu!")
    assertFalse(options().relativenumber)
  }

  @Test
  fun `test number option`() {
    configureByText("\n")
    enterCommand("set scrolloff&")
    assertEquals(0, options().scrolloff)
    assertCommandOutput("set scrolloff?", "  scrolloff=0\n")
    enterCommand("set scrolloff=5")
    assertEquals(5, options().scrolloff)
    assertCommandOutput("set scrolloff?", "  scrolloff=5\n")
  }

  @Test
  fun `test toggle option as a number`() {
    configureByText("\n")
    enterCommand("set number&")
    assertEquals(0, injector.optionGroup.getOptionValue(Options.number, OptionScope.GLOBAL).asDouble().toInt())
    assertCommandOutput("set number?", "nonumber\n")
    enterCommand("let &nu=1000")
    assertEquals(1000, injector.optionGroup.getOptionValue(Options.number, OptionScope.GLOBAL).asDouble().toInt())
    assertCommandOutput("set number?", "  number\n")
  }

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

  @Test
  fun `test string option`() {
    configureByText("\n")
    enterCommand("set selection&")
    assertEquals("inclusive", options().selection)
    assertCommandOutput("set selection?", "  selection=inclusive\n")
    enterCommand("set selection=exclusive")
    assertEquals("exclusive", options().selection)
    assertCommandOutput("set selection?", "  selection=exclusive\n")
  }

  @Test
  fun `test show numbered value`() {
    configureByText("\n")
    assertCommandOutput("set so", "  scrolloff=0\n")
  }

  @Test
  fun `test show numbered value with question mark`() {
    configureByText("\n")
    assertCommandOutput("set so?", "  scrolloff=0\n")
  }
}
