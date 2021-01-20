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

package org.jetbrains.plugins.ideavim.action.motion.select

import com.intellij.openapi.editor.Caret
import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEscapeActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit char mode`() {
    this.doTest(listOf("gh", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit char mode on line start`() {
    this.doTest(listOf("gh", "<esc>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A Discovery

                I${c} found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit char mode on line end`() {
    this.doTest(listOf("gh", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit char mode on file start`() {
    this.doTest(listOf("gh", "<S-Left>", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit char mode on empty line`() {
    this.doTest(listOf("gh", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit char mode multicaret`() {
    this.doTest(listOf("gh", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit in select line mode`() {
    this.doTest(listOf("gH", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit line mode line end`() {
    this.doTest(listOf("gH", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit line mode file start`() {
    this.doTest(listOf("gH", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit line mode empty line`() {
    this.doTest(listOf("gH", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit line mode multicaret`() {
    this.doTest(listOf("gH", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit in select block mode`() {
    this.doTest(listOf("g<C-H>", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertFalse(myFixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    assertEquals(1, myFixture.editor.caretModel.caretCount)
    assertCaretsColour()
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit block mode with motion`() {
    this.doTest(listOf("g<C-H>", "<S-Down>", "<S-Right>", "<esc>"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertFalse(myFixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    assertEquals(1, myFixture.editor.caretModel.caretCount)
    assertCaretsColour()
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit block mode on longer line`() {
    this.doTest(listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(3), "<esc>"),
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
                all rocks and lavender and tufted${c} grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertFalse(myFixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    assertEquals(1, myFixture.editor.caretModel.caretCount)
    assertCaretsColour()
    assertMode(CommandState.Mode.COMMAND)
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test exit block mode on longer line till end`() {
    this.doTest(listOf("g<C-H>", "<S-Down>", "<S-Right>".repeat(5), "<esc>"),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass${c},
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertFalse(myFixture.editor.caretModel.allCarets.any(Caret::hasSelection))
    assertEquals(1, myFixture.editor.caretModel.caretCount)
    assertCaretsColour()
    assertMode(CommandState.Mode.COMMAND)
  }
}
