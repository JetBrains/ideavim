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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.Options.KEYMODEL
import com.maddyhome.idea.vim.option.Options.SELECTMODE
import org.jetbrains.plugins.ideavim.VimListConfig
import org.jetbrains.plugins.ideavim.VimListOptionDefault
import org.jetbrains.plugins.ideavim.VimListOptionTestCase
import org.jetbrains.plugins.ideavim.VimListOptionTestConfiguration

class MotionShiftEndActionTest : VimListOptionTestCase(KEYMODEL, SELECTMODE) {
    @VimListOptionDefault
    fun `test simple end`() {
        val keys = parseKeys("<S-End>")
        val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val after = """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["startsel"]), VimListConfig(SELECTMODE, []))
    fun `test start visual`() {
        val keys = parseKeys("<S-End>")
        val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val after = """
            A Discovery

            I found it in a ${s}legendary land${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["startsel"]), VimListConfig(SELECTMODE, ["key"]))
    fun `test start select`() {
        val keys = parseKeys("<S-End>")
        val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val after = """
            A Discovery

            I found it in a ${s}legendary land${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        doTest(keys, before, after, CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, []), VimListConfig(SELECTMODE, []))
    fun `test continue visual`() {
        val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val after = """
            A Discovery

            ${s}I found it in a legendary land${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("<S-End>"))
        assertState(CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
        typeText(parseKeys("0v", "<S-End>"))
        myFixture.checkResult(after)
        assertState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, []), VimListConfig(SELECTMODE, []))
    fun `test continue select`() {
        val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        val after = """
            A Discovery

            ${s}I found it in a legendary land${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
        configureByText(before)
        typeText(parseKeys("<S-End>"))
        assertState(CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
        typeText(parseKeys("0gh", "<S-End>"))
        myFixture.checkResult(after)
        assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)
    }
}