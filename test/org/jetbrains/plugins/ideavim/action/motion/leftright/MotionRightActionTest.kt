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
import com.maddyhome.idea.vim.option.VirtualEditData
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class MotionRightActionTest : VimOptionTestCase(VirtualEditData.name) {
  @VimOptionDefaultAll
  fun `test simple motion`() {
    doTest("l", """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found i${c}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple motion with repeat`() {
    doTest("3l", """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it ${c}in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple motion to the end`() {
    doTest("3l", """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(VirtualEditData.name, VimTestOptionType.VALUE, [VirtualEditData.onemore]))
  fun `test virtual edit motion to the end`() {
    doTest("3l", """
            Yesterday it worke${c}d
            Today it is not working
            The test is like that.
        """.trimIndent(), """
            Yesterday it worked${c}
            Today it is not working
            The test is like that.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(VirtualEditData.name, VimTestOptionType.VALUE, [VirtualEditData.onemore]))
  fun `test virtual edit motion after dollar`() {
    doTest("\$l", """
            Yesterday it ${c}worked
            Today it is not working
            The test is like that.
        """.trimIndent(), """
            Yesterday it worked${c}
            Today it is not working
            The test is like that.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @VimOptionDefaultAll
  fun `test simple motion non-ascii`() {
    doTest("l", """
            A Discovery

            I found it in a legendar${c}𝛁 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar𝛁${c} land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @VimOptionDefaultAll
  fun `test simple motion emoji`() {
    doTest("l", """
            A Discovery

            I found it in a legendar${c}🐔 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar🐔${c} land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @VimOptionDefaultAll
  fun `test simple motion czech`() {
    doTest("l", """
            A Discovery

            I found it in a legendar${c}ž land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendarž${c} land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test simple motion tab`() {
    doTest("l", """
        A Discovery

        I found it in a legendar${c}. land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass
    """.trimIndent().dotToTab(), """
        A Discovery

        I found it in a legendar.${c} land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass
    """.trimIndent().dotToTab(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimOptionDefaultAll
  fun `test char visual mode`() {
    doTest(listOf("v", "ll"), """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendary lan${s}d${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  @VimOptionDefaultAll
  fun `test block visual mode`() {
    doTest(listOf("<C-V>", "ll"), """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendary lan${s}d${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
  }
}
