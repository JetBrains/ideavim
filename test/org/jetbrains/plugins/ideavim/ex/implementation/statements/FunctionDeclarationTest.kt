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

package org.jetbrains.plugins.ideavim.ex.implementation.statements

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class FunctionDeclarationTest : VimTestCase() {

  fun `test user defined function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function GetHiString(name) |" +
          "  return 'Oh hi ' . a:name | " +
          "endfunction |" +
          "echo GetHiString('Mark')"
      )
    )
    assertExOutput("Oh hi Mark\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test unknown function`() {
    configureByText("\n")
    typeText(commandToKeys("echo GetHiString('Mark')"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E117: Unknown function: GetHiString")
  }

  fun `test nested function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  function F2() |" +
          "    return 555 |" +
          "  endfunction |" +
          "  return 10 * F2() |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    assertExOutput("5550\n")
    typeText(commandToKeys("echo F2()"))
    assertExOutput("555\n")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test call nested function without calling a container function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  function F2() |" +
          "    return 555 |" +
          "  endfunction |" +
          "  return 10 * F2() |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F2()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E117: Unknown function: F2")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test defining an existing function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  return 10 |" +
          "endfunction"
      )
    )
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  return 100 |" +
          "endfunction"
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E122: Function F1 already exists, add ! to replace it")

    typeText(commandToKeys("echo F1()"))
    assertExOutput("10\n")

    typeText(commandToKeys("delf! F1"))
  }

  fun `test redefining an existing function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  return 10 |" +
          "endfunction"
      )
    )
    typeText(
      commandToKeys(
        "" +
          "function! F1() |" +
          "  return 100 |" +
          "endfunction"
      )
    )
    assertPluginError(false)
    typeText(commandToKeys("echo F1()"))
    assertExOutput("100\n")

    typeText(commandToKeys("delf! F1"))
  }
}
