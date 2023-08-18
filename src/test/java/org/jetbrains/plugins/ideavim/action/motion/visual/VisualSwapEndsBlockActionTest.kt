/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class VisualSwapEndsBlockActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test simple block selection SE`() {
    val keys = listOf("<C-V>2e2j", "O")
    val before = """
            A Discovery

            I ${c}|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I ${s}${c}|found|${se} it in a legendary land
            al${s}${c}|l roc|${se}ks and lavender and tufted grass,
            wh${s}${c}|ere i|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
    kotlin.test.assertEquals(LogicalPosition(4, 2), fixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test simple block selection SW`() {
    val keys = listOf("<C-V>2b2j", "O")
    val before = """
            A Discovery

            I |found${c}| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I ${s}|found${c}|${se} it in a legendary land
            al${s}|l roc${c}|${se}ks and lavender and tufted grass,
            wh${s}|ere i${c}|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
    kotlin.test.assertEquals(LogicalPosition(4, 8), fixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test simple block selection NE`() {
    val keys = listOf("<C-V>3e2k", "O")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh${c}|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I ${s}${c}|found|${se} it in a legendary land
            al${s}${c}|l roc|${se}ks and lavender and tufted grass,
            wh${s}${c}|ere i|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
    kotlin.test.assertEquals(LogicalPosition(2, 2), fixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test simple block selection NW`() {
    val keys = listOf("<C-V>3b2k", "O")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i${c}|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I ${s}|found${c}|${se} it in a legendary land
            al${s}|l roc${c}|${se}ks and lavender and tufted grass,
            wh${s}|ere i${c}|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
    kotlin.test.assertEquals(LogicalPosition(2, 8), fixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test with short line`() {
    val keys = listOf("<C-V>2j5e", "O")
    val before = """
            A Discovery

            I found it in a legendary ${c}land{here you can see}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand{some new symbols}
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary ${s}${c}land{here you can s${se}ee}
            all rocks and lavender and${s}${c} tufted grass,${se}
            where it was settled on so${s}${c}me sodden sand{some${se} new symbols}
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
    kotlin.test.assertEquals(LogicalPosition(4, 26), fixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  @Test
  fun `test to long line`() {
    val keys = listOf("<C-V>j5e", "O")
    val before = """
            A Discovery

            I found it in a legendary ${c}land
            all rocks and lavender and tufted grass,[additional characters]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary ${s}${c}land${se}
            all rocks and lavender and${s}${c} tuf${se}ted grass,[additional characters]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
    kotlin.test.assertEquals(LogicalPosition(3, 26), fixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @Test
  fun testVisualSwapEndsBlockActionInBlockMode() {
    typeTextInFile(
      injector.parser.parseKeys("<C-V>" + "2l" + "j" + "O"),
      """
                    a${c}abcc
                    ddeff
                    
      """.trimIndent(),
    )
    assertState(
      """
    a${s}${c}abc${se}c
    d${s}${c}def${se}f
    
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualBlockMovementAfterSwapEndsBlockAction() {
    typeTextInFile(
      injector.parser.parseKeys("<C-V>" + "2l" + "j" + "O" + "k" + "h" + "j"),
      """
                    aabcc
                    d${c}deff
                    gghii
                    jjkll

      """.trimIndent(),
    )
    assertState(
      """
    aabcc
    ${s}${c}ddef${se}f
    ${s}${c}gghi${se}i
    jjkll

      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
    aabcc
    ${s}${c}ddef${se}f
    ${s}${c}gghi${se}i
    ${s}${c}jjkl${se}l

      """.trimIndent(),
    )
  }
}
