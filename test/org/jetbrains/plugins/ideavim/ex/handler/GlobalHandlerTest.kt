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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.VimTestCase

class GlobalHandlerTest : VimTestCase() {
  fun `test default range`() {
    doTest(
      "g/found/d",
      initialText,
      """
            A Discovery

            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass. 
      """.trimIndent(),
    )
  }

  fun `test default range first line`() {
    doTest(
      "g/Discovery/d",
      initialText,
      """

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass. 
      """.trimIndent(),
    )
  }

  fun `test default range last line`() {
    doTest(
      "g/torrent/d",
      initialText,
      """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
      """.trimIndent(),
    )
  }

  fun `test two lines`() {
    doTest(
      "g/it/d",
      initialText,
      """
            A Discovery

            all rocks and lavender and tufted grass,
            hard by the torrent of a mountain pass. 
      """.trimIndent(),
    )
  }

  fun `test two lines force`() {
    doTest(
      "g!/it/d",
      initialText,
      """
            I found it in a legendary land
            where it was settled on some sodden sand
      """.trimIndent(),
    )
  }

  fun `test vglobal`() {
    doTest(
      "v/it/d",
      initialText,
      """
            I found it in a legendary land
            where it was settled on some sodden sand
      """.trimIndent(),
    )
  }

  fun `test current line`() {
    doTest(
      ".g/found/d",
      initialText,
      initialText,
    )
  }

  fun `test current line right place`() {
    doTest(
      ".g/found/d",
      """
                  A Discovery
      
                  I found it in ${c}a legendary land
                  all rocks and lavender and tufted grass,
                  where it was settled on some sodden sand
                  hard by the torrent of a mountain pass. 
      """.trimIndent(),
      """
                A Discovery
    
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass. 
      """.trimIndent(),
    )
  }

  fun `test g only`() {
    doTest(
      "g",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  fun `test g with one separator`() {
    doTest(
      "g/",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  fun `test g with one separator and pattern`() {
    // TODO: 27.05.2021 Implement and check :p command
    doTest(
      "g/found",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  private fun doTest(command: String, before: String, after: String) {
    doTest(listOf(exCommand(command)), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
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
