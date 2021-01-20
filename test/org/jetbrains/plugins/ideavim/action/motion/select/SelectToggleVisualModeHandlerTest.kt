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

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectToggleVisualModeHandlerTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode characterwise`() {
    doTest(listOf("ve", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}found$c$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode characterwise left motion`() {
    doTest(listOf("vb", "<C-G>"),
      """
                A Discovery

                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I $s${c}found i${se}t in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode characterwise empty line`() {
    doTest(listOf("v", "<C-G>"),
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
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode characterwise to line end`() {
    doTest(listOf("vel", "<C-G>"),
      """
                A Discovery

                I found it in a legendary ${c}land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary ${s}land${c}${se}
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode characterwise one letter`() {
    doTest(listOf("v", "<C-G>"),
      """
                A Discovery

                I found it in a legendary ${c}land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary ${s}l${c}${se}and
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode characterwise multicaret`() {
    doTest(listOf("ve", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}found$c$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${s}settled$c$se on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise`() {
    doTest(listOf("gh", "<S-Right>".repeat(4), "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}foun${c}d$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise left motion`() {
    doTest(listOf("gh", "<S-Left>".repeat(5), "<C-G>"),
      """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}${c}foun${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise empty line`() {
    doTest(listOf("gh", "<C-G>"),
      """
                A Discovery
                ${c}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery
                ${s}${c}${se}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise line end`() {
    doTest(listOf("gh", "<S-Right>".repeat(5), "<C-G>"),
      """
                A Discovery

                I found it in a legendary ${c}land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary ${s}lan${c}d${se}
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise one letter`() {
    doTest(listOf("gh", "<C-G>"),
      """
                A Discovery

                I found it in a legendary ${c}land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I found it in a legendary ${s}${c}l${se}and
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @VimBehaviorDiffers(originalVimAfter = """
                A Discovery

                ${s}${c}I${se} found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """)
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise line start`() {
    doTest(listOf("gh", "<S-Left>", "<C-G>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}${c}${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @VimBehaviorDiffers(originalVimAfter = """
                A Discovery

                ${s}I found it in a legendary land
                ${c}a${se}ll rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """)
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise end on line start`() {
    doTest(listOf("gh", "<S-Left>", "<S-Down>", "<C-G>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I found it in a legendary land
                ${c}${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode characterwise multicaret`() {
    doTest(listOf("gh", "<S-Right>".repeat(4), "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}foun${c}d$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${s}sett${c}l${se}ed on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode linewise`() {
    doTest(listOf("Ve", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I foun${c}d it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode linewise up motion`() {
    doTest(listOf("V", "k", "<C-G>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I fo${c}und it in a legendary land
                all rocks and lavender and tufted grass,
                ${se}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode linewise empty line`() {
    doTest(listOf("V", "<C-G>"),
      """
                A Discovery
                ${c}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery
                ${s}${c}
                ${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode linewise multicaret`() {
    doTest(listOf("Ve", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary$c land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I foun${c}d it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                ${s}where it was settle${c}d on some sodden sand
                ${se}hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode linewise`() {
    doTest(listOf("gH", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I ${c}found it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode linewise empty line`() {
    doTest(listOf("gH", "<C-G>"),
      """
                A Discovery
                ${c}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery
                ${s}${c}${se}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode linewise multicaret`() {
    doTest(listOf("gH", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary$c land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                ${s}I ${c}found it in a legendary land$se
                all rocks and lavender and tufted grass,
                ${s}where it was ${c}settled on some sodden sand$se
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_LINE)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode blockwise`() {
    doTest(listOf("<C-V>ejj", "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}found$c$se it in a legendary land
                al${s}l roc$c${se}ks and lavender and tufted grass,
                wh${s}ere i$c${se}t was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to select mode blockwise left motion`() {
    doTest(listOf("<C-V>bjj", "<C-G>"),
      """
                A Discovery

                I found${c} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}${c}found ${se}it in a legendary land
                al${s}${c}l rock${se}s and lavender and tufted grass,
                wh${s}${c}ere it${se} was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.SELECT,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode blockwise`() {
    doTest(listOf("g<C-H>", "<S-Right>".repeat(4), "<S-Down>".repeat(2), "<C-G>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I ${s}foun${c}d$se it in a legendary land
                al${s}l ro${c}c${se}ks and lavender and tufted grass,
                wh${s}ere ${c}i${se}t was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_BLOCK)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test switch to visual mode blockwise to left`() {
    doTest(listOf("g<C-H>", "<S-Left>".repeat(4), "<S-Down>".repeat(2), "<C-G>"),
      """
                A Discovery

                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      """
                A Discovery

                I fou$s${c}nd ${se}it in a legendary land
                all r$s${c}ock${se}s and lavender and tufted grass,
                where$s${c} it${se} was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
      CommandState.Mode.VISUAL,
      CommandState.SubMode.VISUAL_BLOCK)
  }
}
