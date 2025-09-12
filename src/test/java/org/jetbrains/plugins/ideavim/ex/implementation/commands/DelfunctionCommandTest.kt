/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DelfunctionCommandTest : VimTestCase() {

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test delete existing function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        (
          "function F1() |" +
            "  return 10 |" +
            "endfunction"
          ),
      ),
    )
    typeText(commandToKeys("delfunction F1"))
    assertPluginError(false)
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test delf`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        (
          "function F1() |" +
            "  return 10 |" +
            "endfunction"
          ),
      ),
    )
    typeText(commandToKeys("delf F1"))
    assertPluginError(false)
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test delete nonexistent function`() {
    configureByText("\n")
    typeText(commandToKeys("delfunction F1"))
    assertPluginError(true)
    assertPluginErrorMessage("E130: Unknown function: F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test delete script function from command line context`() {
    configureByText("\n")
    typeText(commandToKeys("delfunction s:F1"))
    assertPluginError(true)
    assertPluginErrorMessage("E81: Using <SID> not in a script context")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test delete nonexistent function with ignoreError flag`() {
    configureByText("\n")
    typeText(commandToKeys("delfunction! F1"))
    assertPluginError(false)
  }
}
