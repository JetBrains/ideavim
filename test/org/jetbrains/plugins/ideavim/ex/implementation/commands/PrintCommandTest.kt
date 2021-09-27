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
