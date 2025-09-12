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
import org.junit.jupiter.api.Test

class FunctionCallTest : VimTestCase() {

  @Test
  fun `test function as method call`() {
    configureByText("\n")
    typeText(commandToKeys("echo -4->abs()"))
    assertExOutput("4")
  }

  @Test
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
    assertExOutput("81")

    typeText(commandToKeys("delfunction! Power2"))
  }

  @Test
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
    assertExOutput("42")

    typeText(commandToKeys("delfunction! Subtraction"))
  }

  @Test
  fun `test function as method call with lambda`() {
    configureByText("\n")
    typeText(commandToKeys("echo 52->{x,y -> x-y}(10)"))
    assertExOutput("42")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
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
    assertPluginErrorMessage("E46: Cannot change read-only variable \"a:number\"")

    typeText(commandToKeys("delfunction! ThrowException"))
  }

  @Test
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
    assertExOutput("81")

    typeText(commandToKeys("delfunction! Power2"))
  }
}
