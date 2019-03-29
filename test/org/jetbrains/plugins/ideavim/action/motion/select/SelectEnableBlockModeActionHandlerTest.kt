package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEnableBlockModeActionHandlerTest : VimTestCase() {
    fun `test entering select mode`() {
        doTest(parseKeys("g<C-H>"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                ${s}I$c$se found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }

    fun `test entering select mode at the end of file`() {
        doTest(parseKeys("g<C-H>"),
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
                hard by the torrent of a mountain pass$s.$c$se""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }

    fun `test entering select mode on empty line`() {
        doTest(parseKeys("g<C-H>"),
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
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }

    fun `test entering select mode multicaret`() {
        doTest(parseKeys("g<C-H>"),
                """
                A Discovery
                $c
                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${s}s$c${se}ettled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
    }
}