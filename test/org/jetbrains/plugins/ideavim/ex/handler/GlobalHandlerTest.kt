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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.HistoryGroup
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
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

  fun `test nested global`() {
    doTest(
      "g/found/v/notfound/d",
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

  fun `test nested multiple lines`() {
    doTest(
      "g/it/v/notit/d",
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
                hard by the torrent of a mountain pass. 
      """.trimIndent(),
    )
  }

  fun `test check history`() {
    VimPlugin.getHistory().clear()
    val initialEntries = VimPlugin.getHistory().getEntries(HistoryGroup.COMMAND, 0, 0)
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
    val entries = VimPlugin.getHistory().getEntries(HistoryGroup.COMMAND, 0, 0)
    TestCase.assertEquals(1, entries.size - initialEntries.size)
    val element = entries.last()
    TestCase.assertEquals("g/found/d", element.entry)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test g only`() {
    doTest(
      "g",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test g with one separator`() {
    doTest(
      "g/",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test g with one separator and pattern`() {
    doTest(
      "g/found",
      initialText,
      initialText,
    )
    assertExOutput("I found it in a legendary land\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test g with one separator and pattern and separator`() {
    doTest(
      "g/found",
      initialText,
      initialText,
    )
    assertExOutput("I found it in a legendary land\n")
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
