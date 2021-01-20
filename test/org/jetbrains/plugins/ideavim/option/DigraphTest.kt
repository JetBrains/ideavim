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

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
// TODO: 2019-06-18 VimOptionsTestCase
class DigraphTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph`() {
    OptionsManager.digraph.set()

    doTest("i B<BS>B", """
            A Discovery

            I found it${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it ¦${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph stops`() {
    OptionsManager.digraph.set()

    doTest("i B<BS>BHello", """
            A Discovery

            I found it${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it ¦Hello${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph double backspace`() {
    OptionsManager.digraph.set()

    doTest("i B<BS><BS>B", """
            A Discovery

            I found it${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found itB${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph backspace digraph`() {
    OptionsManager.digraph.set()

    doTest("i B<BS>B<BS>B", """
            A Discovery

            I found it${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it B${c} in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }
}
