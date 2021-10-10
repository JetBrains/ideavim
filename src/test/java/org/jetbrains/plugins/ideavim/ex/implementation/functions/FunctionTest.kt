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

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class FunctionTest : VimTestCase() {

  fun `test function for built-in function`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs')"))
    typeText(commandToKeys("echo Ff(-10)"))
    assertExOutput("10\n")

    typeText(commandToKeys("echo Ff"))
    assertExOutput("abs\n")
  }

  fun `test function with arglist`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs', [-10])"))
    typeText(commandToKeys("echo Ff()"))
    assertExOutput("10\n")

    typeText(commandToKeys("echo Ff"))
    assertExOutput("function('abs', [-10])\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test function for unknown function`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('unknown')"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E700: Unknown function: unknown")
  }

  // todo in release 1.9 (good example of multiple exceptions at once)
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test function with wrong function name`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function(32)"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E129: Function name required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test function with wrong second argument`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs', 10)"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E923: Second argument of function() must be a list or a dict")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test function with wrong third argument`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs', [], 40)"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E922: expected a dict")
  }

  fun `test redefining a function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! SayHi() |
        echo 'hello' |
      endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let Ff = function('SayHi')"))
    typeText(commandToKeys("call Ff()"))
    assertExOutput("hello\n")

    typeText(
      commandToKeys(
        """
      function! SayHi() |
        echo 'hi' |
      endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("call Ff()"))
    assertExOutput("hi\n")

    typeText(commandToKeys("delfunction! SayHi"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test deleting function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! SayHi() |
        echo 'hello' |
      endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let Ff = function('SayHi')"))
    typeText(commandToKeys("delfunction! SayHi"))
    typeText(commandToKeys("call Ff()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E933: Function was deleted: SayHi")
  }
}
