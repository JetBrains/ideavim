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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.option.KeyModelOptionData
import com.maddyhome.idea.vim.option.SelectModeOptionData
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class MotionShiftRightActionHandlerTest : VimOptionTestCase(KeyModelOptionData.name, SelectModeOptionData.name) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.startsel]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test visual right`() {
    doTest(listOf("<S-Right>"),
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
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.startsel]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test visual right twice`() {
    doTest(listOf("<S-Right><S-Right>"),
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
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.startsel]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [SelectModeOptionData.key])
  )
  fun `test select right`() {
    doTest(listOf("<S-Right>"),
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
      CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.startsel]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [SelectModeOptionData.key])
  )
  fun `test select right twice`() {
    doTest(listOf("<S-Right><S-Right>"),
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
      CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test simple motion char mode`() {
    doTest(listOf("gh", "<S-Right>"),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test at the lineend char mode`() {
    doTest(listOf("gh", "<S-Right>"),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test out of line char mode`() {
    doTest(listOf("gh", "<S-Right>".repeat(2)),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test file end char mode`() {
    doTest(listOf("gh", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$s.$c$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test file char mode multicaret`() {
    doTest(listOf("gh", "<S-Right>".repeat(2)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.""".trimIndent(),
      """
                A Discovery

                I ${s}fou$c${se}nd it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$s.$c$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test simple motion line mode`() {
    doTest(listOf("gH", "<S-Right>"),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test lineend line mode`() {
    doTest(listOf("gH", "<S-Right>"),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test out of line line mode`() {
    doTest(listOf("gH", "<S-Right>".repeat(2)),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test fileend line mode`() {
    doTest(listOf("gH", "<S-Right>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the torrent of a mountain pass.$c$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test line mode multicaret`() {
    doTest(listOf("gH", "<S-Right>"),
      """
                A Discovery

                I found ${c}it in ${c}a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.""".trimIndent(),
      """
                A Discovery

                ${s}I found i${c}t in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the torrent of a mountain pass.$c$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test simple motion block mode`() {
    doTest(listOf("g<C-H>", "<S-Right>"),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test at the lineend block mode`() {
    doTest(listOf("g<C-H>", "<S-Right>"),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test out of line block mode`() {
    doTest(listOf("g<C-H>", "<S-Right>".repeat(2)),
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
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test file end block mode`() {
    doTest(listOf("g<C-H>", "<S-Right>".repeat(2)),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$s.$c$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test to longer line block mode`() {
    doTest(listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(3)),
      """
                A Discovery

                I found it in a legendary lan${c}d
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary lan${s}d$se
                all rocks and lavender and tu${s}fted$c$se grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continuevisual]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test continuevisual`() {
    doTest(listOf("v", "<S-Right>".repeat(3)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}fou${c}n${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, []),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test no continueselect`() {
    doTest(listOf("gh", "<S-Right>".repeat(3)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}found it in ${c}${se}a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, []),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test no continuevisual`() {
    doTest(listOf("v", "<S-Right>".repeat(3)),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}found it in ${c}a${se} legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }
}
