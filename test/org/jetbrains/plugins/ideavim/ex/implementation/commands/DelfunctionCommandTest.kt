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

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class DelfunctionCommandTest : VimTestCase() {

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test delete existing function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        (
          "function F1() |" +
            "  return 10 |" +
            "endfunction"
          )
      )
    )
    typeText(commandToKeys("delfunction F1"))
    assertPluginError(false)
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E117: Unknown function: F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test delf`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        (
          "function F1() |" +
            "  return 10 |" +
            "endfunction"
          )
      )
    )
    typeText(commandToKeys("delf F1"))
    assertPluginError(false)
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E117: Unknown function: F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test delete nonexistent function`() {
    configureByText("\n")
    typeText(commandToKeys("delfunction F1"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E130: Unknown function: F1")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test delete nonexistent function with ignoreError flag`() {
    configureByText("\n")
    typeText(commandToKeys("delfunction! F1()"))
    assertPluginError(false)
  }
}
