/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption

@TraceOptions(TestOptionConstants.keymodel, TestOptionConstants.selectmode)
class MotionShiftRightActionHandlerTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test visual right`() {
    doTest(
      listOf("<S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}f${c}o${se}und it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test visual right twice`() {
    doTest(
      listOf("<S-Right><S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}fo${c}u${se}nd it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test visual shift right in Insert mode enters Insert Visual mode`() {
    doTest(
      listOf("i<S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}f${c}o${se}und it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test visual shift right in Replace mode enters Replace Visual mode`() {
    doTest(
      listOf("R<S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}f${c}o${se}und it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.REPLACE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [OptionConstants.selectmode_key]),
  )
  fun `test select right`() {
    doTest(
      listOf("<S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}f${c}${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [OptionConstants.selectmode_key]),
  )
  fun `test select right twice`() {
    doTest(
      listOf("<S-Right><S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}fo${c}${se}und it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [OptionConstants.selectmode_key]),
  )
  fun `test select shift right in Insert enters Insert Select mode`() {
    doTest(
      listOf("i<S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}f${c}${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [OptionConstants.selectmode_key]),
  )
  fun `test select shift right in Replace enters Replace Select mode`() {
    doTest(
      listOf("R<S-Right>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}f${c}${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE, returnTo = Mode.REPLACE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test simple motion char mode`() {
    doTest(
      listOf("gh", "<S-Right>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I $c${se}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test at the lineend char mode`() {
    doTest(
      listOf("gh", "<S-Right>"),
      """
                A Discovery

                I found it in a legendary la${c}nd
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary la${s}nd$c$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test out of line char mode`() {
    doTest(
      listOf("gh", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lan${s}d$c$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test file end char mode`() {
    doTest(
      listOf("gh", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$s.$c$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test file char mode multicaret`() {
    doTest(
      listOf("gh", "<S-Right>".repeat(2)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                A Discovery

                I ${s}fou$c${se}nd it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$s.$c$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test simple motion line mode`() {
    doTest(
      listOf("gH", "<S-Right>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I$c found it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test lineend line mode`() {
    doTest(
      listOf("gH", "<S-Right>"),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I found it in a legendary land$c
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test out of line line mode`() {
    doTest(
      listOf("gH", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I found it in a legendary land$c
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test fileend line mode`() {
    doTest(
      listOf("gH", "<S-Right>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the torrent of a mountain pass.$c$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test line mode multicaret`() {
    doTest(
      listOf("gH", "<S-Right>"),
      """
                A Discovery

                I found ${c}it in ${c}a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                A Discovery

                ${s}I found i${c}t in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the torrent of a mountain pass.$c$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test simple motion block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Right>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I $c${se}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test at the lineend block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Right>"),
      """
                A Discovery

                I found it in a legendary la${c}nd
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary la${s}nd$c$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test out of line block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lan${s}d$c$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test file end block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$s.$c$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continueselect]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test to longer line block mode`() {
    doTest(
      listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(3)),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary lan${s}d$se
                all rocks and lavender and tu${s}fted$c$se grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.BLOCK_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_continuevisual]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test continuevisual`() {
    doTest(
      listOf("v", "<S-Right>".repeat(3)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}fou${c}n${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [""]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test no continueselect`() {
    doTest(
      listOf("gh", "<S-Right>".repeat(3)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}found it in ${c}${se}a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [""]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  fun `test no continuevisual`() {
    doTest(
      listOf("v", "<S-Right>".repeat(3)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I ${s}found it in ${c}a${se} legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
