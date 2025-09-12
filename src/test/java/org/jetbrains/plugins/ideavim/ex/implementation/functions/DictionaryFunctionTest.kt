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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DictionaryFunctionTest : VimTestCase() {

  @Test
  fun `test self in dictionary function with assignment via function function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.data |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'data': [], 'print': function('Print')}"))
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("[]")

    typeText(commandToKeys("delfunction! Print"))
  }

  @Test
  fun `test self in dictionary function with assignment via let command`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict_name'}"))
    typeText(commandToKeys("let PrintFr = function('Print')"))
    typeText(commandToKeys("let dict.print = PrintFr"))
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict_name")

    typeText(commandToKeys("delfunction! Print"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test dictionary function without dict`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("call Print()"))
    assertPluginError(true)
    assertPluginErrorMessage("E725: Calling dict function without Dictionary: Print")

    typeText(commandToKeys("delfunction! Print"))
  }

  // todo big brain logic
  @Test
  @Disabled
  fun `test assigned dictionary function to another dictionary`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("echo dict.print"))
    assertExOutput("function('Print', {'name': 'dict', 'print': function('Print')}")
    typeText(commandToKeys("echo dict"))
    assertExOutput("{'name': 'dict', 'print': function('Print')}")
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict")

    typeText(commandToKeys("let dict2 = {'name': 'dict2', 'print': dict.print}"))
    typeText(commandToKeys("echo dict2.print"))
    assertExOutput("function('Print', {'name': 'dict2', 'print': function('Print', {name: 'dict', 'print': function('Print')})}")
    typeText(commandToKeys("echo dict2"))
    assertExOutput("{'name': 'dict2', 'print': function('Print', {name: 'dict', 'print': function('Print')})}")
    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict2")

    typeText(commandToKeys("delfunction! Print"))
  }

  @Test
  fun `test self is not changed after let assignment`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("let dict2 = {'name': 'dict2'}"))
    typeText(commandToKeys("let dict2.print = dict.print"))

    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict2")

    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict")

    typeText(commandToKeys("delfunction! Print"))
  }

  @Test
  fun `test self is not changed after in-dictionary assignment`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("let dict2 = {'name': 'dict2', 'print': dict.print}"))

    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict2")

    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict")

    typeText(commandToKeys("delfunction! Print"))
  }

  @Test
  fun `test assigned partial to another dictionary`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict'}"))
    typeText(commandToKeys("let dict.print = function('Print', dict)"))
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict")

    typeText(commandToKeys("let dict2 = {'name': 'dict2', 'print': dict.print}"))
    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict")

    typeText(commandToKeys("delfunction! Print"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test self is read-only`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             let self = [] |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("call dict.print()"))
    assertPluginError(true)
    assertPluginErrorMessage("E46: Cannot change read-only variable \"self\"")

    typeText(commandToKeys("delfunction! Print"))
  }

  @Test
  fun `test self in inner dictionary`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'innerDict': {'name': 'innerDict', 'print': function('Print')}}"))
    typeText(commandToKeys("call dict.innerDict.print()"))
    assertExOutput("innerDict")

    typeText(commandToKeys("delfunction! Print"))
  }
}
