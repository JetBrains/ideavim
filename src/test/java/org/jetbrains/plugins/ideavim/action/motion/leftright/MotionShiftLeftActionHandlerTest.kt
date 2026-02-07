/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionShiftLeftActionHandlerTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual left`() {
    doTest(
      listOf("<S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fou${s}${c}nd${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual left twice`() {
    doTest(
      listOf("<S-Left><S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fo${s}${c}und${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual shift left in Insert enters Insert Visual mode`() {
    doTest(
      listOf("i<S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fou${s}${c}nd${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT),
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual shift left in Replace enters Replace Visual mode`() {
    doTest(
      listOf("R<S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fou${s}${c}nd${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.REPLACE),
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test select left`() {
    doTest(
      listOf("<S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fou${s}${c}n${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=key")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test select left twice`() {
    doTest(
      listOf("<S-Left><S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fo${s}${c}un${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=key")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test select shift left in Insert enters Insert Visual mode`() {
    doTest(
      listOf("i<S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fou${s}${c}n${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT),
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=key")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test select shift left in Replace enters Replace Visual mode`() {
    doTest(
      listOf("R<S-Left>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fou${s}${c}n${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE, returnTo = Mode.REPLACE),
    ) {
      enterCommand("set keymodel=startsel")
      enterCommand("set selectmode=key")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test simple motion char mode`() {
    doTest(
      listOf("gh", "<S-Left>"),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I f$s$c${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test double motion char mode`() {
    doTest(
      listOf("gh", "<S-Left>".repeat(2)),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I $s${c}f${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test at line start char mode`() {
    doTest(
      listOf("gh", "<S-Left>".repeat(2)),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                $s$c${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test at file start char mode`() {
    doTest(
      listOf("gh", "<S-Left>".repeat(2)),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                $s$c${se}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test char mode multicaret`() {
    doTest(
      listOf("gh", "<S-Left>".repeat(2)),
      """
                ${c}A Discovery

                I found$c it in a legendary land
                all rocks and lavender and tufted grass$c,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                $s$c${se}A Discovery

                I foun$s${c}d$se it in a legendary land
                all rocks and lavender and tufted gras$s${c}s$se,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test simple motion line mode`() {
    doTest(
      listOf("gH", "<S-Left>"),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I ${c}found it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test to line start line mode`() {
    doTest(
      listOf("gH", "<S-Left>".repeat(5)),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                $s${c}I found it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test to file start line mode`() {
    doTest(
      listOf("gH", "<S-Left>".repeat(5)),
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                $s${c}A Discovery
                ${se}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test line mode multicaret`() {
    doTest(
      listOf("gH", "<S-Left>".repeat(5)),
      """
                A ${c}Discovery

                I found it in a ${c}legendary land
                all rocks and ${c}lavender and tufted grass$c,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                $s${c}A Discovery
                ${se}
                ${s}I found it ${c}in a legendary land
                ${se}${s}all rocks$c and lavender and tufted grass,
                ${se}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test simple motion block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Left>"),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I f$s$c${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test twice motion block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Left>".repeat(2)),
      """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I $s${c}f${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test at line start block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Left>".repeat(2)),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                $s$c${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test at file start block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Left>".repeat(2)),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                $s$c${se}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test multiline with empty line block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Left>".repeat(2)),
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                ${s}A ${se}Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
    assertCaretsVisualAttributes()
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test multiline block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Down>".repeat(2), "<S-Left>".repeat(3)),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I fo$s${c}un${se}d it in a legendary land
                all $s${c}ro${se}cks and lavender and tufted grass,
                wher$s${c}e ${se}it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    ) {
      enterCommand("set keymodel=continueselect")
      enterCommand("set selectmode=")
    }
    assertCaretsVisualAttributes()
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test continuevisual`() {
    doTest(
      listOf("v", "<S-Left>".repeat(3)),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I f${s}${c}ound${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set keymodel=continuevisual")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test no continueselect`() {
    doTest(
      listOf("gh", "<S-Left>".repeat(3)),
      """
                A Discovery

                I found it in a ${c}legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it ${s}${c}in a ${se}legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=")
      enterCommand("set selectmode=")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test no continuevisual`() {
    doTest(
      listOf("v", "<S-Left>".repeat(3)),
      """
                A Discovery

                I found it in a ${c}legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found ${s}${c}it in a l${se}egendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set keymodel=")
      enterCommand("set selectmode=")
    }
  }
}
