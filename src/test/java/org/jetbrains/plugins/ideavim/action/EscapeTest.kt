/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.idea.TestFor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class EscapeTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-3190"])
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `mapping to control esc`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    typeText(commandToKeys("nmap <C-Esc> k"))
    typeText("<C-Esc>")

    assertState(
      """
      Lorem Ipsum
      $c
      Lorem ipsum dolor sit amet, consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-3190"])
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `mapping to alt esc`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    typeText(commandToKeys("nmap <A-Esc> k"))
    typeText("<A-Esc>")

    assertState(
      """
      Lorem Ipsum
      $c
      Lorem ipsum dolor sit amet, consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-3190"])
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `mapping to shift esc`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    typeText(commandToKeys("nmap <S-Esc> k"))
    typeText("<S-Esc>")

    assertState(
      """
      Lorem Ipsum
      $c
      Lorem ipsum dolor sit amet, consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )
  }
}