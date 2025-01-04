/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select

import com.intellij.openapi.editor.Caret
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SelectEscapeActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode`() {
    this.doTest(
      listOf("gh", "<esc>"),
      """
                A Discovery

                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found i${c}t in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode on line start`() {
    this.doTest(
      listOf("gh", "<esc>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I$c found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode on line end puts caret at correct offset when returning to Normal mode`() {
    this.doTest(
      listOf("gh", "<esc>"),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode on line end puts caret at correct offset when returning to Insert mode`() {
    this.doTest(
      listOf("i", "<S-Right>", "<esc>"),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land${c}
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set selectmode=key keymodel=startsel")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode on file start`() {
    this.doTest(
      listOf("gh", "<S-Left>", "<esc>"),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode on empty line`() {
    this.doTest(
      listOf("gh", "<esc>"),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit char mode multicaret`() {
    this.doTest(
      listOf("gh", "<esc>"),
      """
                A Discovery

                I ${c}found it ${c}in a legendary land
                all rocks ${c}and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I f${c}ound it i${c}n a legendary land
                all rocks a${c}nd lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit in select line mode`() {
    this.doTest(
      listOf("gH", "<esc>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit line mode line end`() {
    this.doTest(
      listOf("gH", "<esc>"),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit line mode file start`() {
    this.doTest(
      listOf("gH", "<esc>"),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit line mode empty line`() {
    this.doTest(
      listOf("gH", "<esc>"),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit line mode multicaret`() {
    this.doTest(
      listOf("gH", "<esc>"),
      """
                A Discovery

                I ${c}found it ${c}in a legendary land
                all rocks ${c}and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks ${c}and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit in select block mode`() {
    this.doTest(
      listOf("g<C-H>", "<esc>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    kotlin.test.assertFalse(fixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    kotlin.test.assertEquals(1, fixture.editor.caretModel.caretCount)
    assertCaretsVisualAttributes()
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit block mode with motion`() {
    this.doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>", "<esc>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    kotlin.test.assertFalse(fixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    kotlin.test.assertEquals(1, fixture.editor.caretModel.caretCount)
    assertCaretsVisualAttributes()
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit block mode on longer line`() {
    this.doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(3), "<esc>"),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted$c grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    kotlin.test.assertFalse(fixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    kotlin.test.assertEquals(1, fixture.editor.caretModel.caretCount)
    assertCaretsVisualAttributes()
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test exit block mode on longer line till end`() {
    this.doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(5), "<esc>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass$c,
                where it was settled on some sodden sand123
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand12${c}3
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    kotlin.test.assertFalse(fixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    kotlin.test.assertEquals(1, fixture.editor.caretModel.caretCount)
    assertCaretsVisualAttributes()
  }
}
