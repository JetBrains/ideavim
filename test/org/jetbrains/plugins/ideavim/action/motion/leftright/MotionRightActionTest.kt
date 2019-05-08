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

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviourDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionRightActionTest : VimTestCase() {
    fun `test simple motion`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found i<caret>t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    fun `test simple motion with repeat`() {
        doTest(parseKeys("3l"), """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it <caret>in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    fun `test simple motion to the end`() {
        doTest(parseKeys("3l"), """
            A Discovery

            I found it in a legendary lan<caret>d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendary lan<caret>d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    @VimBehaviourDiffers("""
            A Discovery

            I found it in a legendar𝛁<caret> land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """)
    fun `test simple motion non-ascii`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found it in a legendar<caret>𝛁 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar<caret>𝛁 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    @VimBehaviourDiffers("""
            A Discovery

            I found it in a legendar🐔<caret> land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """)
    fun `test simple motion emoji`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found it in a legendar<caret>🐔 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar<caret>🐔 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    fun `test simple motion tab`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found it in a legendar<caret>	 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar	<caret> land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }
}