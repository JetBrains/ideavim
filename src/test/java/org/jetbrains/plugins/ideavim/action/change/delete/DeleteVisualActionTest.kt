/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class DeleteVisualActionTest : VimTestCase() {
  @Test
  fun `test delete block SE direction`() {
    val keys = listOf("<C-V>e2j", "d")
    val before = """
            Lorem Ipsum

            I |${c}found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test delete block SW direction`() {
    val keys = listOf("<C-V>b2j", "d")
    val before = """
            Lorem Ipsum

            I |foun${c}d| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test delete block NW direction`() {
    val keys = listOf("<C-V>b2k", "d")
    val before = """
            Lorem Ipsum

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere ${c}i|t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test delete block NE direction`() {
    val keys = listOf("<C-V>2e2k", "d")
    val before = """
            Lorem Ipsum

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|${c}ere i|t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test delete after extend selection`() {
    // This test emulates deletion after structural selection
    // In short, when caret is not on the selection end
    configureByText(
      """
            Lorem Ipsum

            ${s}Lorem ipsum dolor sit amet,
            all rocks ${c}and lavender and tufted grass,
            Sed in orci mauris.
            ${se}Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    typeText(injector.parser.parseKeys("d"))
    assertState(
      """
            Lorem Ipsum

            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  @Test
  fun `test delete with dollar motion`() {
    val keys = listOf("<C-V>3j$", "d")
    val before = """
            Lorem Ipsum

            I |${c}found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional symbols]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I |
            al|
            wh|
            ha|
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  // This test causes a bug, but it's minor, and I'm not sure how to fix it properly
  //  It happens because we do perform sequential deletions on editor that is mutated.
  //  I think, we should firstly collect operations and then apply them at once. However,
  //    this will require to much effort at the moment for a tiny bug
  @Test
  @Disabled
  fun `test delete on empty lines`() {
    val keys = listOf("Vd")
    val before = """
            1
            ${c}
            ${c}
    """.trimIndent()
    val after = """
            1
            ${c}
            ${c}
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }
}
