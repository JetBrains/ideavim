/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.idea.TestFor
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.newapi.vim
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
  fun `test match ignores case`() {
    doTest(
      exCommand("g/test/p"),
      """
        |one test
        |two
        |three Test
        |four
        |five TEST
      """.trimMargin(),
      """
        |one test
        |two
        |three Test
        |four
        |five TEST
      """.trimMargin()
    ) {
      enterCommand("set ignorecase")
    }
    assertOutput(
      """
        |one test
        |three Test
        |five TEST
      """.trimMargin()
    )
  }

  @Test
  fun `test check history`() {
    VimPlugin.getHistory().clear()
    val initialEntries = VimPlugin.getHistory().getEntries(VimHistory.Type.Command, 0, 0)
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
    val entries = VimPlugin.getHistory().getEntries(VimHistory.Type.Command, 0, 0)
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
    assertOutput(
      """
      g/found
      I found it in a legendary land""".trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print multiple matching line if no command`() {
    doTest(
      "g/it",
      initialText,
      initialText,
    )
    assertOutput(
      """
      |g/it
      |I found it in a legendary land
      |where it was settled on some sodden sand
      """.trimMargin()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print multiple matching line if no command with number option`() {
    configureByText(initialText)
    val editor = fixture.editor.vim
    ApplicationManager.getApplication().invokeAndWait {
      injector.options(editor).number = true
    }
    typeText(commandToKeys(":g/it"))
    assertOutput(
      """
      |g/it
      |3 I found it in a legendary land
      |5 where it was settled on some sodden sand
      """.trimMargin()
    )
    ApplicationManager.getApplication().invokeAndWait {
      injector.options(editor).number = false
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print matching lines if no command and no trailing separator`() {
    doTest(
      "g/found/",
      initialText,
      initialText,
    )
    assertOutput(
      """
      g/found/
      I found it in a legendary land""".trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print matching lines if no command and no trailing separator with number option`() {
    configureByText(initialText)
    val editor = fixture.editor.vim
    ApplicationManager.getApplication().invokeAndWait {
      injector.options(editor).number = true
    }
    typeText(commandToKeys(":g/found/"))
    assertOutput(
      """
      g/found/
      3 I found it in a legendary land""".trimIndent()
    )
    ApplicationManager.getApplication().invokeAndWait {
      injector.options(editor).number = false
    }
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

  @Test
  @TestFor(issues = ["VIM-3517"])
  fun `test global used as a creative substitute alternative`() {
    val textBefore = """
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
      """.trimIndent()
    val textAfter = """
replacement bbb ccc replacement replacement
bbbbbb
bbbbbb
replacement bbb ccc replacement replacement replacement replacement replacement
bbbbbb
bbbbbb
bbbbbb
bbbbbb
bbbbbb
end
      """.trimIndent()
    configureByText(textBefore)
    typeText(commandToKeys(":g/aaa"))
    assertState(textBefore)
    typeText(commandToKeys(":g//s//replacement/g"))
    assertState(textAfter)
  }

  private fun doTest(command: String, before: String, after: String) {
    doTest(listOf(exCommand(command)), before, after)
  }
}
