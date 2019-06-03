package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEnableLineModeActionHandlerTest : VimTestCase() {
    fun `test entering select mode`() {
        doTest(parseKeys("gH"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $s${c}I found it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }

    fun `test entering select mode at the end of file`() {
        doTest(parseKeys("gH"),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.""".trimIndent(),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the torrent of a mountain pass$c.$se""".trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }

    fun `test entering select mode on empty line`() {
        doTest(parseKeys("gH"),
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
                CommandState.SubMode.VISUAL_LINE)
    }

    fun `test entering select mode multicaret`() {
        doTest(parseKeys("gH"),
                """
                A Discovery
                $c
                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on ${c}some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery
                $s$c$se
                $s${c}I found it in a legendary land$se
                all rocks and lavender and tufted grass,
                ${s}where it was ${c}settled on some sodden sand$se
                hard by the torrent of a mountain pass.""".trimIndent(),
                CommandState.Mode.SELECT,
                CommandState.SubMode.VISUAL_LINE)
    }
}