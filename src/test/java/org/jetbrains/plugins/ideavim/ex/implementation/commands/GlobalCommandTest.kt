/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.history.HistoryConstants
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class GlobalCommandTest : VimTestCase() {
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
  fun `test delete search term in default range of whole file`() {
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
  fun `test delete first line in default range`() {
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
  fun `test delete last line in default range`() {
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
  fun `test delete multiple matching lines`() {
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
  fun `test delete multiple non-matching lines with global-bang`() {
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
  fun `test delete multiple non-matching lines with vglobal`() {
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
  fun `test delete nothing if not found in current line`() {
    doTest(
      ".g/found/d",
      initialText,
      initialText,
    )
  }

  @Test
  fun `test delete current line if matching`() {
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

  // VIM-3348
  @Test
  fun `test substitution`() {
    doTest(
      "g/red cow/s//green horse/g",
      """
        |in the big race a red cow is in the lead
        |in the big race a red car is in the lead
        |in the big race a red cow is in the lead
      """.trimMargin(),
      """
        |in the big race a green horse is in the lead
        |in the big race a red car is in the lead
        |in the big race a green horse is in the lead
      """.trimMargin()
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
  fun `test print matching line if no command`() {
    doTest(
      "g/found",
      initialText,
      initialText,
    )
    assertExOutput("I found it in a legendary land")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print multiple matching line if no command`() {
    doTest(
      "g/it",
      initialText,
      initialText,
    )
    assertExOutput("""
      |I found it in a legendary land
      |where it was settled on some sodden sand
      """.trimMargin())
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print matching lines if no command and no trailing separator`() {
    doTest(
      "g/found/",
      initialText,
      initialText,
    )
    assertExOutput("I found it in a legendary land")
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

  @Test
  @TestFor(issues = ["VIM-3501"])
  fun `test global is executed once per line`() {
    doTest(
      "g/aaa/d",
      """
aaa bbb ccc aaa aaa
bbbbbb
bbbbbb
aaa bbb ccc aaa aaa aaa aaa aaa
bbbbbb
bbbbbb
bbbbbb
bbbbbb
bbbbbb
end
      """.trimIndent(),
      """
bbbbbb
bbbbbb
bbbbbb
bbbbbb
bbbbbb
bbbbbb
bbbbbb
end
      """.trimIndent(),
    )
  }

  private fun doTest(command: String, before: String, after: String) {
    doTest(listOf(exCommand(command)), before, after, Mode.NORMAL())
  }
}
