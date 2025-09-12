/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.statements

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ForTest : VimTestCase() {

  @Test
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
        """.trimIndent(),
      ),
    )
    assertExOutput("aabbccddeeff")
  }

  @Test
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
        """.trimIndent(),
      ),
    )
    assertExOutput("hello")
  }

  @Test
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
        """.trimIndent(),
      ),
    )
    assertExOutput("hello!!")
  }

  @Test
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
        """.trimIndent(),
      ),
    )
    assertExOutput("123")
  }

  @Test
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
        """.trimIndent(),
      ),
    )
    assertExOutput("3")
  }

  @Test
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
        """.trimIndent(),
      ),
    )
    assertExOutput("123 abc")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
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
        """.trimIndent(),
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E714: List required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
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
        """.trimIndent(),
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E714: List required")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
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
        """.trimIndent(),
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E688: More targets than List items")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
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
        """.trimIndent(),
      ),
    )
    assertPluginError(true)
    assertPluginErrorMessage("E684: Less targets than List items")
  }

  // todo in 1.9: test for with different default scopes
}
