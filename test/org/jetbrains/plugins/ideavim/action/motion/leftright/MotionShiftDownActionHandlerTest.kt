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

class MotionShiftDownActionHandlerTest : VimTestCase() {
    fun `test visual down`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Down>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}found it in a legendary land
                al${c}l${se} rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test visual down twice`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Down><S-Down>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${c}e${se}re it was settled on some sodden sand
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

        doTest(parseKeys("<S-Down><S-Down><S-Down>"),
                """
                A Discovery

                I found it in a legendary land[additional chars${c}]
                all rocks and lavender and tufted grass,[additional chars]
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.[additional chars]
                """.trimIndent(),
                """
                A Discovery

                I found it in a legendary land[additional chars${s}]
                all rocks and lavender and tufted grass,[additional chars]
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.[additio${c}n${se}al chars]
                """.trimIndent(),
                CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test select down`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }
        Options.getInstance().getListOption(Options.SELECTMODE)?.set("key") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Down>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}found it in a legendary land
                al${c}${se}l rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
        )
    }

    fun `test select down twice`() {
        Options.getInstance().getListOption(Options.KEYMODEL)?.set("startsel") ?: run {
            TestCase.fail()
            return
        }
        Options.getInstance().getListOption(Options.SELECTMODE)?.set("key") ?: run {
            TestCase.fail()
            return
        }

        doTest(parseKeys("<S-Down><S-Down>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                """
                A Discovery

                I ${s}found it in a legendary land
                all rocks and lavender and tufted grass,
                wh${c}${se}ere it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent(),
                CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER
        )
    }
}