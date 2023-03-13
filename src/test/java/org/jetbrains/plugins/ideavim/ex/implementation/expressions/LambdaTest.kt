/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
    )
    assertExOutput("5 - 2 = 3\n")
  }

  fun `test lambda with more arguments than needed`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let L = { x -> x + 10 } |
          echo L(32, 100)
        """.trimIndent(),
      ),
    )
    assertPluginError(false)
    assertExOutput("42\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test lambda with less arguments than needed`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let L = { x -> x + 10 } |
          echo L()
        """.trimIndent(),
      ),
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
