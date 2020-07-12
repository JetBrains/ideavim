/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class MotionArrowDownActionTest : VimOptionTestCase(KeyModelOptionData.name) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  fun `test visual default options`() {
    doTest(listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                wher${c}e${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.stopsel]))
  fun `test visual stopsel`() {
    doTest(listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.stopselect]))
  fun `test visual stopselect`() {
    doTest(listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                wher${c}e${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.stopvisual]))
  fun `test visual stopvisual`() {
    doTest(listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.stopvisual]))
  fun `test visual stopvisual multicaret`() {
    doTest(listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the t${c}orrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, []))
  fun `test char select stopsel`() {
    doTest(listOf("gh", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                where${c}${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.stopselect]))
  fun `test char select simple move`() {
    doTest(listOf("gH", "<Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(KeyModelOptionData.name, VimTestOptionType.LIST, [KeyModelOptionData.stopselect]))
  fun `test select multiple carets`() {
    doTest(listOf("gH", "<Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }
}
