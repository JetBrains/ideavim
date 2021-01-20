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

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.option.KeyModelOptionData
import com.maddyhome.idea.vim.option.SelectModeOptionData
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class MotionShiftDownActionHandlerTest : VimOptionTestCase(KeyModelOptionData.name, SelectModeOptionData.name) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.startsel]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test visual down`() {
    doTest(listOf("<S-Down>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I ${s}found it in a legendary land
                al${c}l${se} rocks and lavender and tufted grass,
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
  fun `test visual down twice`() {
    doTest(listOf("<S-Down><S-Down>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I ${s}found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${c}e${se}re it was settled on some sodden sand
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
  fun `test save column`() {
    doTest(listOf("<S-Down><S-Down><S-Down>"),
      """
                A Discovery

                I found it in a legendary land[additional chars${c}]
                all rocks and lavender and tufted grass,[additional chars]
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.[additional chars]
                """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land[additional chars${s}]
                all rocks and lavender and tufted grass,[additional chars]
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.[additio${c}n${se}al chars]
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.startsel]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [SelectModeOptionData.key])
  )
  fun `test select down`() {
    doTest(listOf("<S-Down>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I ${s}found it in a legendary land
                al${c}${se}l rocks and lavender and tufted grass,
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
  fun `test select down twice`() {
    doTest(listOf("<S-Down><S-Down>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I ${s}found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${c}${se}ere it was settled on some sodden sand
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
  fun `test char select simple move`() {
    doTest(listOf("gh", "<S-Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A Discovery

                ${s}I found it in a legendary land
                a$c${se}ll rocks and lavender and tufted grass,
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
  fun `test char select move to empty line`() {
    doTest(listOf("gh", "<S-Down>"),
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A ${s}Discovery
                $c$se
                I found it in a legendary land
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
  fun `test char select move from empty line`() {
    doTest(listOf("gh", "<S-Down>"),
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
                $s
                $c${se}I found it in a legendary land
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
  fun `test char select move to file end`() {
    doTest(listOf("gh", "<S-Down>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard ${c}by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard ${s}b$c${se}y the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test char select move multicaret`() {
    doTest(listOf("gh", "<S-Down>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard ${c}by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}found it in a legendary land
                all$c$se rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard ${s}b$c${se}y the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test line select simple move`() {
    doTest(listOf("gH", "<S-Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A Discovery

                ${s}I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                ${se}where it was settled on some sodden sand
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
  fun `test line select to empty line`() {
    doTest(listOf("gH", "<S-Down>"),
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                ${s}A Discovery
                $c
                ${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
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
  fun `test line select from empty line`() {
    doTest(listOf("gH", "<S-Down>"),
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
                $s
                ${c}I found it in a legendary land
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
  fun `test line select to file end`() {
    doTest(listOf("gH", "<S-Down>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the ${c}torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the ${c}torrent of a mountain pass.$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test line select multicaret`() {
    doTest(listOf("gH", "<S-Down>"),
      """
                A Discovery

                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the ${c}torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I found it in a legendary land
                all rock${c}s and lavender and tufted grass,
                ${se}where it was settled on some sodden sand
                ${s}hard by the ${c}torrent of a mountain pass.$se""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test block select simple move`() {
    doTest(listOf("g<C-H>", "<S-Down>"),
      """
                A Discovery

                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found ${s}i$c${se}t in a legendary land
                all rock${s}s$c$se and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test block select to empty line`() {
    doTest(listOf("g<C-H>", "<S-Down>"),
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                ${s}A ${se}Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test block select from empty line`() {
    doTest(listOf("g<C-H>", "<S-Down>"),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery
                $s$c$se
                $s$c${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.continueselect]),
    VimTestOption(SelectModeOptionData.name, VimTestOptionType.LIST, [])
  )
  fun `test block select to file end`() {
    doTest(listOf("g<C-H>", "<S-Down>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the ${c}torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the ${s}t$c${se}orrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }
}
