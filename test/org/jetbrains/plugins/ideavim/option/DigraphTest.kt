/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.Options
import com.maddyhome.idea.vim.option.ToggleOption
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class DigraphTest : VimTestCase() {
    fun `test digraph`() {
        (Options.getInstance().getOption("digraph") as ToggleOption).set()

        doTest(parseKeys("i B<BS>B"), """
            A Discovery

            I found it<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it ¦<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `test digraph stops`() {
        (Options.getInstance().getOption("digraph") as ToggleOption).set()

        doTest(parseKeys("i B<BS>BHello"), """
            A Discovery

            I found it<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it ¦Hello<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `test digraph double backspace`() {
        (Options.getInstance().getOption("digraph") as ToggleOption).set()

        doTest(parseKeys("i B<BS><BS>B"), """
            A Discovery

            I found it<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found itB<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `test digraph backspace digraph`() {
        (Options.getInstance().getOption("digraph") as ToggleOption).set()

        doTest(parseKeys("i B<BS>B<BS>B"), """
            A Discovery

            I found it<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it B<caret> in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }
}