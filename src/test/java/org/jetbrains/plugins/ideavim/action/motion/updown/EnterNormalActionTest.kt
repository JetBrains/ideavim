/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.intellij.idea.TestFor
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class EnterNormalActionTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-3190"])
  fun `mapping to control enter`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    typeText(commandToKeys("nmap <C-Enter> k"))
    typeText("<C-Enter>")

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
  fun `mapping to alt enter`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    typeText(commandToKeys("nmap <A-Enter> k"))
    typeText("<A-Enter>")

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
  fun `mapping to shift enter`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    typeText(commandToKeys("nmap <S-Enter> k"))
    typeText("<S-Enter>")

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