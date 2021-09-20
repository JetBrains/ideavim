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

  fun `test closure function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 5 |" +
          "  function F2() closure |" +
          "    return 10 * x |" +
          "  endfunction |" +
          "  return F2() |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    assertExOutput("50\n")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  fun `test outer variable cannot be reached from inner function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 5 |" +
          "  function F2() |" +
          "    return 10 * x |" +
          "  endfunction |" +
          "  return F2() |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: x")
    assertNoExOutput()

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  fun `test call closure function multiple times`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 0 |" +
          "  function F2() closure |" +
          "    let x += 1 |" +
          "    return x |" +
          "  endfunction |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    typeText(commandToKeys("echo F2()"))
    assertExOutput("1\n")
    typeText(commandToKeys("echo F2()"))
    assertExOutput("2\n")
    typeText(commandToKeys("echo F2()"))
    assertExOutput("3\n")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  fun `test local variables exist after delfunction command`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  let x = 0 |" +
          "  function F2() closure |" +
          "    let x += 1 |" +
          "    return x |" +
          "  endfunction |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    typeText(commandToKeys("echo F2()"))
    assertExOutput("1\n")
    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("echo F2()"))
    assertExOutput("2\n")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test outer function does not see inner closure function variable`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function F1() |" +
          "  function! F2() closure |" +
          "    let x = 1 |" +
          "    return 10 |" +
          "  endfunction |" +
          "  echo x |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: x")

    typeText(commandToKeys("echo F2()"))
    assertExOutput("10\n")
    assertPluginError(false)

    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: x")

    typeText(commandToKeys("delf! F1"))
    typeText(commandToKeys("delf! F2"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test function without abort flag`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function! F1() |" +
          "  echo unknownVar |" +
          "  let g:x = 10 |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: unknownVar")

    typeText(commandToKeys("echo x"))
    assertExOutput("10\n")
    assertPluginError(false)

    typeText(commandToKeys("delf! F1"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test function with abort flag`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "function! F1() abort |" +
          "  echo unknownVar |" +
          "  let g:x = 10 |" +
          "endfunction"
      )
    )
    typeText(commandToKeys("echo F1()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: unknownVar")

    typeText(commandToKeys("echo x"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E121: Undefined variable: x")

    typeText(commandToKeys("delf! F1"))
  }
}
