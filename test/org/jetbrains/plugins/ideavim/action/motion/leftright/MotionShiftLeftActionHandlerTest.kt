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
import com.maddyhome.idea.vim.option.Options.*
import org.jetbrains.plugins.ideavim.VimListConfig
import org.jetbrains.plugins.ideavim.VimListOptionTestCase
import org.jetbrains.plugins.ideavim.VimListOptionTestConfiguration

class MotionShiftLeftActionHandlerTest : VimListOptionTestCase(KEYMODEL, SELECTMODE) {
    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["startsel"]), VimListConfig(SELECTMODE, []))
    fun `test visual left`() {
        doTest(parseKeys("<S-Left>"),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I fou${s}${c}nd${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["startsel"]), VimListConfig(SELECTMODE, []))
    fun `test visual left twice`() {
        doTest(parseKeys("<S-Left><S-Left>"),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I fo${s}${c}und${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["startsel"]), VimListConfig(SELECTMODE, ["key"]))
    fun `test select left`() {
        doTest(parseKeys("<S-Left>"),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I fou${s}${c}n${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["startsel"]), VimListConfig(SELECTMODE, ["key"]))
    fun `test select left twice`() {
        doTest(parseKeys("<S-Left><S-Left>"),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I fo${s}${c}un${se}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test simple motion char mode`() {
        doTest(parseKeys("gh", "<S-Left>"),
                """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I f$s$c${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test double motion char mode`() {
        doTest(parseKeys("gh", "<S-Left>".repeat(2)),
                """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I $s${c}f${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test at line start char mode`() {
        doTest(parseKeys("gh", "<S-Left>".repeat(2)),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $s$c${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test at file start char mode`() {
        doTest(parseKeys("gh", "<S-Left>".repeat(2)),
                """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s$c${se}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test char mode multicaret`() {
        doTest(parseKeys("gh", "<S-Left>".repeat(2)),
                """
                ${c}A Discovery

                I found$c it in a legendary land
                all rocks and lavender and tufted grass$c,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s$c${se}A Discovery

                I foun$s${c}d$se it in a legendary land
                all rocks and lavender and tufted gras$s${c}s$se,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test simple motion line mode`() {
        doTest(parseKeys("gH", "<S-Left>"),
                """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                ${s}I ${c}found it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test to line start line mode`() {
        doTest(parseKeys("gH", "<S-Left>".repeat(5)),
                """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $s${c}I found it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test to file start line mode`() {
        doTest(parseKeys("gH", "<S-Left>".repeat(5)),
                """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s${c}A Discovery
                ${se}
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test line mode multicaret`() {
        doTest(parseKeys("gH", "<S-Left>".repeat(5)),
                """
                A ${c}Discovery

                I found it in a ${c}legendary land
                all rocks and ${c}lavender and tufted grass$c,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s${c}A Discovery
                ${se}
                ${s}I found it ${c}in a legendary land
                ${se}${s}all rocks$c and lavender and tufted grass,
                ${se}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test simple motion block mode`() {
        doTest(parseKeys("g<C-H>", "<S-Left>"),
                """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I f$s$c${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_BLOCK)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test twice motion block mode`() {
        doTest(parseKeys("g<C-H>", "<S-Left>".repeat(2)),
                """
                A Discovery

                I f${c}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I $s${c}f${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_BLOCK)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test at line start block mode`() {
        doTest(parseKeys("g<C-H>", "<S-Left>".repeat(2)),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $s$c${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_BLOCK)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test at file start block mode`() {
        doTest(parseKeys("g<C-H>", "<S-Left>".repeat(2)),
                """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s$c${se}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_BLOCK)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test multiline with empty line block mode`() {
        doTest(parseKeys("g<C-H>", "<S-Down>", "<S-Left>".repeat(2)),
                """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                ${s}A ${se}Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinueselect"]), VimListConfig(SELECTMODE, []))
    fun `test multiline block mode`() {
        doTest(parseKeys("g<C-H>", "<S-Down>".repeat(2), "<S-Left>".repeat(3)),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I fo$s${c}un${se}d it in a legendary land
                all $s${c}ro${se}cks and lavender and tufted grass,
                wher$s${c}e ${se}it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, ["acontinuevisual"]), VimListConfig(SELECTMODE, []))
    fun `test acontinuevisual`() {
        doTest(parseKeys("v", "<S-Left>".repeat(3)),
                """
                A Discovery

                I foun${c}d it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I f${s}${c}ound${se} it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.VISUAL,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, []), VimListConfig(SELECTMODE, []))
    fun `test no acontinueselect`() {
        doTest(parseKeys("gh", "<S-Left>".repeat(3)),
                """
                A Discovery

                I found it in a ${c}legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I found it ${s}${c}in a ${se}legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_CHARACTER)
    }

    @VimListOptionTestConfiguration(VimListConfig(KEYMODEL, []), VimListConfig(SELECTMODE, []))
    fun `test no acontinuevisual`() {
        getInstance().getListOption(KEYMODEL)!!.set("")
        doTest(parseKeys("v", "<S-Left>".repeat(3)),
                """
                A Discovery

                I found it in a ${c}legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I found ${s}${c}it in a l${se}egendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.VISUAL,
                CommandState.SubMode.VISUAL_CHARACTER)
    }
}