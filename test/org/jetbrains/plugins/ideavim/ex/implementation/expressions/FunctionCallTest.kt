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

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class FunctionCallTest : VimTestCase() {

  fun `test function as method call`() {
    configureByText("\n")
    typeText(commandToKeys("echo -4->abs()"))
    assertExOutput("4\n")
  }

  fun `test chained function as method call`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! Power2(number) |
        return a:number * a:number |
      endfunction |
      echo -3->abs()->Power2()->Power2()
        """.trimIndent()
      )
    )
    assertExOutput("81\n")

    typeText(commandToKeys("delfunction! Power2"))
  }

  fun `test function as method call with args`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! Subtraction(minuend, subtrahend) |
        return a:minuend - a:subtrahend |
      endfunction |
      echo 52->Subtraction(10)
        """.trimIndent()
      )
    )
    assertExOutput("42\n")

    typeText(commandToKeys("delfunction! Subtraction"))
  }

  fun `test function as method call with lambda`() {
    configureByText("\n")
    typeText(commandToKeys("echo 52->{x,y -> x-y}(10)"))
    assertExOutput("42\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test read-only variable`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          function! ThrowException(number) |
            let a:number = 20 |
          endfunction |
          call ThrowException(20)
        """.trimIndent()
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E46: Cannot change read-only variable \"a:number\"")

    typeText(commandToKeys("delfunction! ThrowException"))
  }

  fun `test dict function call`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
        function! Power2(number) |
          return a:number * a:number |
        endfunction |
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'power': function('Power2')}"))
    typeText(commandToKeys("echo dict.power(9)"))
    assertExOutput("81\n")

    typeText(commandToKeys("delfunction! Power2"))
  }
}
