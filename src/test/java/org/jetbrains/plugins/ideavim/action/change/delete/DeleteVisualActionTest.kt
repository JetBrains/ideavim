/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class DeleteVisualActionTest : VimTestCase() {
  @Test
  fun `test delete block SE direction`() {
    val keys = listOf("<C-V>e2j", "d")
    val before = """
            A Discovery

            I |${c}found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test delete block SW direction`() {
    val keys = listOf("<C-V>b2j", "d")
    val before = """
            A Discovery

            I |foun${c}d| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test delete block NW direction`() {
    val keys = listOf("<C-V>b2k", "d")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere ${c}i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test delete block NE direction`() {
    val keys = listOf("<C-V>2e2k", "d")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|${c}ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test delete after extend selection`() {
    // This test emulates deletion after structural selection
    // In short, when caret is not on the selection end
    configureByText(
      """
            A Discovery

            ${s}I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            ${se}hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, VimStateMachine.Mode.VISUAL)
    typeText(injector.parser.parseKeys("d"))
    assertState(
      """
            A Discovery

            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test delete with dollar motion`() {
    val keys = listOf("<C-V>3j$", "d")
    val before = """
            A Discovery

            I |${c}found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional symbols]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I |
            al|
            wh|
            ha|
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }
}
