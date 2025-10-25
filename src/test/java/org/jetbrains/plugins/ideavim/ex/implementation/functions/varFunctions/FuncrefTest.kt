/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.varFunctions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FuncrefTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test funcref for built-in function`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = funcref('abs')"))
    assertPluginError(true)
    assertPluginErrorMessage("E700: Unknown function: abs")
  }

  @Test
  fun `test funcref with arglist`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          function! Abs(number) |
            return abs(a:number) |
          endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let Ff = funcref('Abs', [-10])"))
    typeText(commandToKeys("echo Ff()"))
    assertExOutput("10")

    typeText(commandToKeys("echo Ff"))
    assertExOutput("function('Abs', [-10])")

    typeText(commandToKeys("delfunction! Abs"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test funcref for unknown function`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = funcref('Unknown')"))
    assertPluginError(true)
    assertPluginErrorMessage("E700: Unknown function: Unknown")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test funcref with wrong function name`() {
    configureByText("\n")
    typeText(commandToKeys("let Ff = funcref(32)"))
    assertPluginError(true)
    assertPluginErrorMessage("E129: Function name required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test funcref with wrong second argument`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          function! Abs(number) |
            return abs(a:number) |
          endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let Ff = funcref('Abs', 10)"))
    assertPluginError(true)
    assertPluginErrorMessage("E923: Second argument of function() must be a list or a dict")

    typeText(commandToKeys("delfunction! Abs"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test funcref with wrong third argument`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          function! Abs(number) |
            return abs(a:number) |
          endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let Ff = funcref('Abs', [], 40)"))
    assertPluginError(true)
    assertPluginErrorMessage("E922: Expected a dict")

    typeText(commandToKeys("delfunction! Abs"))
  }

  @Test
  fun `test funcref with arglist and dictionary`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      function! PrintSum(x) dict |
        return a:x + self.offset |
      endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let d = {'offset': 100}"))
    typeText(commandToKeys("let Ff = funcref('PrintSum', [42], d)"))
    typeText(commandToKeys("echo Ff()"))
    assertExOutput("142")

    typeText(commandToKeys("delfunction! PrintSum"))
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
    typeText(commandToKeys("let Ff = funcref('SayHi')"))
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
    assertExOutput("hello")

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
    typeText(commandToKeys("let Ff = funcref('SayHi')"))
    typeText(commandToKeys("delfunction! SayHi"))
    typeText(commandToKeys("call Ff()"))
    assertPluginError(true)
    assertPluginErrorMessage("E933: Function was deleted: SayHi")
  }
}
