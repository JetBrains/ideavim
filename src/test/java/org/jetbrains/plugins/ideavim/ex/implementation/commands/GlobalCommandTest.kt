/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.history.HistoryConstants
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class GlobalCommandTest : VimTestCase() {
  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
  fun `test current line`() {
    doTest(
      ".g/found/d",
      initialText,
      initialText,
    )
  }

  @Test
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

  @Test
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

  @Test
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

  @Test
  fun `test check history`() {
    VimPlugin.getHistory().clear()
    val initialEntries = VimPlugin.getHistory().getEntries(HistoryConstants.COMMAND, 0, 0)
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
    val entries = VimPlugin.getHistory().getEntries(HistoryConstants.COMMAND, 0, 0)
    kotlin.test.assertEquals(1, entries.size - initialEntries.size)
    val element = entries.last()
    kotlin.test.assertEquals("g/found/d", element.entry)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test g only`() {
    doTest(
      "g",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test g with one separator`() {
    doTest(
      "g/",
      initialText,
      initialText,
    )
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test g with one separator and pattern`() {
    doTest(
      "g/found",
      initialText,
      initialText,
    )
    assertExOutput("I found it in a legendary land\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test g with one separator and pattern and separator`() {
    doTest(
      "g/found",
      initialText,
      initialText,
    )
    assertExOutput("I found it in a legendary land\n")
  }

  private fun doTest(command: String, before: String, after: String) {
    doTest(listOf(exCommand(command)), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
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

  @Test
  fun `test bar in command`() {
    doTest(
      "g/\\vfound|rocks/d",
      """
                  A Discovery
      
                  I found it in ${c}a legendary land
                  all rocks and lavender and tufted grass,
                  where it was settled on some sodden sand
                  hard by the torrent of a mountain pass. 
      """.trimIndent(),
      """
                A Discovery
    
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass. 
      """.trimIndent(),
    )
  }
}
