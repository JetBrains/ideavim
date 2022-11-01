/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase

class PrintCommandTest : VimTestCase() {
  fun `test default range`() {
    configureByText(initialText)
    typeText(commandToKeys("p"))
    assertExOutput("A Discovery\n")
  }

  fun `test default range with P`() {
    configureByText(initialText)
    typeText(commandToKeys("P"))
    assertExOutput("A Discovery\n")
  }

  fun `test full text`() {
    configureByText(initialText)
    typeText(commandToKeys("%p"))
    assertExOutput(initialText)
  }

  fun `test with count`() {
    configureByText(initialText)
    typeText(commandToKeys("p 3"))
    assertExOutput(
      """
                A Discovery
    
                I found it in a legendary land
                
      """.trimIndent()
    )
  }

  companion object {
    private val initialText = """
                A Discovery
    
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass. 
    """.trimIndent()
  }
}
