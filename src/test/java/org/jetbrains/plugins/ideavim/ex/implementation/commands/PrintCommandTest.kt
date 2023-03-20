/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class PrintCommandTest : VimTestCase() {
  @Test
  fun `test default range`() {
    configureByText(initialText)
    typeText(commandToKeys("p"))
    assertExOutput("Lorem Ipsum\n")
  }

  @Test
  fun `test default range with P`() {
    configureByText(initialText)
    typeText(commandToKeys("P"))
    assertExOutput("Lorem Ipsum\n")
  }

  @Test
  fun `test full text`() {
    configureByText(initialText)
    typeText(commandToKeys("%p"))
    assertExOutput(initialText)
  }

  @Test
  fun `test with count`() {
    configureByText(initialText)
    typeText(commandToKeys("p 3"))
    assertExOutput(
      """
                Lorem Ipsum
    
                Lorem ipsum dolor sit amet,
                
      """.trimIndent(),
    )
  }

  companion object {
    private val initialText = """
                Lorem Ipsum
    
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
  }
}
