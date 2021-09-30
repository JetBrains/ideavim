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

class LambdaTest : VimTestCase() {

  fun `test lambda with no args`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let L = {-> "hello from an empty function"} |
          echo L()
        """.trimIndent()
      )
    )
    assertExOutput("hello from an empty function\n")
  }

  fun `test lambda with one arg`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let L = { x -> x + 10 } |
          echo L(32)
        """.trimIndent()
      )
    )
    assertExOutput("42\n")
  }

  fun `test lambda with multiple arguments`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let Addition = { x, y -> x + y } |
          echo Addition(-142, 184)
        """.trimIndent()
      )
    )
    assertExOutput("42\n")
  }

  fun `test lambda's name`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let Addition = { x, y -> x + y } |
          let Subtraction = { x, y -> x - y} |
          echo Addition
        """.trimIndent()
      )
    )
    assertExOutput("function('<lambda>0')\n")

    typeText(commandToKeys("echo Subtraction"))
    assertExOutput("function('<lambda>1')\n")
  }

  fun `test lambda is a closure function`() {
    // in this test we test that we can access outer function variables from lambda
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          function! Subtraction(minuend, subtrahend) |
            let Subtract = { x, y -> x - y} |
            echo a:minuend .. ' - ' .. a:subtrahend .. ' = ' .. Subtract(a:minuend, a:subtrahend) |
          endfunction |
          call Subtraction(5, 2)
        """.trimIndent()
      )
    )
    assertExOutput("5 - 2 = 3\n")
  }

  // todo maybe create new lambda handler after refactoring?..
  // redundant args should be ignored
//  fun `test lambda with more arguments than needed`() {
//    configureByText("\n")
//    typeText(
//      commandToKeys(
//        """
//          let L = { x -> x + 10 } |
//          echo L(32, 100)
//        """.trimIndent()
//      )
//    )
//    assertPluginError(false)
//    assertExOutput("42\n")
//  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test lambda with less arguments than needed`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let L = { x -> x + 10 } |
          echo L()
        """.trimIndent()
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E119: Not enough arguments for function: <lambda>0")
  }

  fun `test lambda function call with no args`() {
    configureByText("\n")
    typeText(commandToKeys("echo {-> 'hello from an empty function'}()"))
    assertExOutput("hello from an empty function\n")
  }

  fun `test lambda function call with args`() {
    configureByText("\n")
    typeText(commandToKeys("echo {x, y -> x*y}(6, 7)"))
    assertExOutput("42\n")
  }
}
