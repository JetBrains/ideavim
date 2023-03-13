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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
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
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'power': function('Power2')}"))
    typeText(commandToKeys("echo dict.power(9)"))
    assertExOutput("81\n")

    typeText(commandToKeys("delfunction! Power2"))
  }
}
