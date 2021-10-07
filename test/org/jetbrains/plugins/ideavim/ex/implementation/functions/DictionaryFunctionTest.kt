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

class DictionaryFunctionTest : VimTestCase() {

  fun `test self in dictionary function with assignment via function function`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.data |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'data': [], 'print': function('Print')}"))
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("[]\n")

    typeText(commandToKeys("delfunction! Print"))
  }

  fun `test self in dictionary function with assignment via let command`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'name': 'dict_name'}"))
    typeText(commandToKeys("let PrintFr = function('Print')"))
    typeText(commandToKeys("let dict.print = PrintFr"))
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict_name\n")

    typeText(commandToKeys("delfunction! Print"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test dictionary function without dict`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("call Print()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E725: Calling dict function without Dictionary: Print")

    typeText(commandToKeys("delfunction! Print"))
  }

  // todo big brain logic
//  fun `test assigned dictionary function to another dictionary`() {
//    configureByText("\n")
//    typeText(
//      commandToKeys(
//        """
//           function Print() dict |
//             echo self.name |
//           endfunction
//        """.trimIndent()
//      )
//    )
//    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
//    typeText(commandToKeys("echo dict.print"))
//    assertExOutput("function('Print', {'name': 'dict', 'print': function('Print')}")
//    typeText(commandToKeys("echo dict"))
//    assertExOutput("{'name': 'dict', 'print': function('Print')}")
//    typeText(commandToKeys("call dict.print()"))
//    assertExOutput("dict\n")
//
//    typeText(commandToKeys("let dict2 = {'name': 'dict2', 'print': dict.print}"))
//    typeText(commandToKeys("echo dict2.print"))
//    assertExOutput("function('Print', {'name': 'dict2', 'print': function('Print', {name: 'dict', 'print': function('Print')})}")
//    typeText(commandToKeys("echo dict2"))
//    assertExOutput("{'name': 'dict2', 'print': function('Print', {name: 'dict', 'print': function('Print')})}")
//    typeText(commandToKeys("call dict2.print()"))
//    assertExOutput("dict2\n")
//
//    typeText(commandToKeys("delfunction! Print"))
//  }

  fun `test self is not changed after let assignment`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("let dict2 = {'name': 'dict2'}"))
    typeText(commandToKeys("let dict2.print = dict.print"))

    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict2\n")

    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict\n")

    typeText(commandToKeys("delfunction! Print"))
  }

  fun `test self is not changed after in-dictionary assignment`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("let dict2 = {'name': 'dict2', 'print': dict.print}"))

    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict2\n")

    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict\n")

    typeText(commandToKeys("delfunction! Print"))
  }

  fun `test assigned partial to another dictionary`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'name': 'dict'}"))
    typeText(commandToKeys("let dict.print = function('Print', dict)"))
    typeText(commandToKeys("call dict.print()"))
    assertExOutput("dict\n")

    typeText(commandToKeys("let dict2 = {'name': 'dict2', 'print': dict.print}"))
    typeText(commandToKeys("call dict2.print()"))
    assertExOutput("dict\n")

    typeText(commandToKeys("delfunction! Print"))
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test self is read-only`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             let self = [] |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'print': function('Print')}"))
    typeText(commandToKeys("call dict.print()"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E46: Cannot change read-only variable \"self\"")

    typeText(commandToKeys("delfunction! Print"))
  }

  fun `test self in inner dictionary`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
           function Print() dict |
             echo self.name |
           endfunction
        """.trimIndent()
      )
    )
    typeText(commandToKeys("let dict = {'name': 'dict', 'innerDict': {'name': 'innerDict', 'print': function('Print')}}"))
    typeText(commandToKeys("call dict.innerDict.print()"))
    assertExOutput("innerDict\n")

    typeText(commandToKeys("delfunction! Print"))
  }
}
