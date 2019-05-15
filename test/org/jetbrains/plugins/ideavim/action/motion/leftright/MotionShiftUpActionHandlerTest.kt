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
import com.maddyhome.idea.vim.option.Options
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionShiftUpActionHandlerTest : VimTestCase() {
    fun `test visual up`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                al${c}l rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}${c}found it in a legendary land
                all${se} rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test visual up twice`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Up><S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${c}ere it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                whe${se}re it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test save column`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Up><S-Up><S-Up>"),
                """
                A Discovery

                I found it in a legendary land[additional chars]
                all rocks and lavender and tufted grass,[additional chars]
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.[additio${c}nal chars]
                """.trimIndent(),
                """
                A Discovery

                I found it in a legendary land[additional chars${s}${c}]
                all rocks and lavender and tufted grass,[additional chars]
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.[addition${se}al chars]
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test select up`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }
        Options.getInstance().getListOption(Options.SELECTMODE)?.set("key") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                al${c}l rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}${c}found it in a legendary land
                al${se}l rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test select up twice`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }
        Options.getInstance().getListOption(Options.SELECTMODE)?.set("key") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Up><S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${c}ere it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${se}ere it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
        )
    }
}