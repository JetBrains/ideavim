/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.implementation.statements

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class ForTest : VimTestCase() {

  fun `test iterating over string`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      let alphabet = 'abcdef' |
      let result = '' |
      for i in alphabet |
          let result .= i . i |
      endfor |
      echo result
        """.trimIndent()
      )
    )
    assertExOutput("aabbccddeeff\n")
  }

  fun `test iterating over list`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      let result = '' |
      for char in ['h', 'e', 'l', 'l', 'o'] |
          let result .= char |
      endfor |
      echo result
        """.trimIndent()
      )
    )
    assertExOutput("hello\n")
  }

  fun `test iterating over modifying list`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      let result = '' |
      let list = ['h', 'e', 'l', 'l', 'o'] |
      for char in list |
          let result .= char |
          if char == 'l' |
              let list += ['!'] |
          endif |
      endfor |
      echo result
        """.trimIndent()
      )
    )
    assertExOutput("hello!!\n")
  }

  fun `test continue`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      let result = '' |
      for char in 'afa1k2janm3' |
        if char < '0' || char > '9' |
          continue |
        endif |
        let result .= char |
      endfor |
      echo result
        """.trimIndent()
      )
    )
    assertExOutput("123\n")
  }

  fun `test break`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
      let firstDigitIndex = '' |
      let counter = 0 |
      for char in 'afa1k2janm3' |
        if char >= '0' && char <= '9' |
          let firstDigitIndex = counter |
          break |
        endif |
        let counter += 1 |
      endfor |
      echo firstDigitIndex
        """.trimIndent()
      )
    )
    assertExOutput("3\n")
  }

  fun `test for with list`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let firstElements = '' |
          let secondElements = '' |
          for [f, s] in [[1, 'a'], [2, 'b'], [3, 'c']] |
            let firstElements .= f |
            let secondElements .= s |
          endfor |
          echo firstElements .. ' ' .. secondElements
        """.trimIndent()
      )
    )
    assertExOutput("123 abc\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test for with list and non-list iterable`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let firstElements = '' |
          let secondElements = '' |
          for [f, s] in 'abcdef' |
            let firstElements .= f |
            let secondElements .= s |
          endfor |
        """.trimIndent()
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E714: List required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test for with list and non-list iterable item`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let firstElements = '' |
          let secondElements = '' |
          for [f, s] in ['ab', [1, 2]] |
            let firstElements .= f |
            let secondElements .= s |
          endfor |
        """.trimIndent()
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E714: List required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test for with list and different length iterable item 1`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let firstElements = '' |
          let secondElements = '' |
          for [f, s] in [[1]] |
            let firstElements .= f |
            let secondElements .= s |
          endfor |
        """.trimIndent()
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E688: More targets than List items")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test for with list and different length iterable item 2`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          let firstElements = '' |
          let secondElements = '' |
          for [f, s] in [[1, 2, 4]] |
            let firstElements .= f |
            let secondElements .= s |
          endfor |
        """.trimIndent()
      )
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E684: Less targets than List items")
  }

  // todo in 1.9: test for with different default scopes
}
