/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FunctionTest : VimTestCase() {

  @Test
  fun `test function for built-in function`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs')"))
    typeText(commandToKeys("echo Ff(-10)"))
    assertExOutput("10")

    typeText(commandToKeys("echo Ff"))
    assertExOutput("abs")
  }

  @Test
  fun `test function with arglist`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs', [-10])"))
    typeText(commandToKeys("echo Ff()"))
    assertExOutput("10")

    typeText(commandToKeys("echo Ff"))
    assertExOutput("function('abs', [-10])")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test function for unknown function`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('unknown')"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E700: Unknown function: unknown")
  }

  // todo in release 1.9 (good example of multiple exceptions at once)
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test function with wrong function name`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function(32)"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E129: Function name required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test function with wrong second argument`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs', 10)"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E923: Second argument of function() must be a list or a dict")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test function with wrong third argument`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = function('abs', [], 40)"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E922: expected a dict")
  }

  @Test
  fun `test redefining a function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! SayHi() |
        echo 'hello' |
      endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let Ff = function('SayHi')"))
    typeText(commandToKeys("call Ff()"))
    assertExOutput("hello")

    typeText(
      commandToKeys(
        """
      function! SayHi() |
        echo 'hi' |
      endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("call Ff()"))
    assertExOutput("hi")

    typeText(commandToKeys("delfunction! SayHi"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test deleting function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! SayHi() |
        echo 'hello' |
      endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let Ff = function('SayHi')"))
    typeText(commandToKeys("delfunction! SayHi"))
    typeText(commandToKeys("call Ff()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E933: Function was deleted: SayHi")
  }
}
