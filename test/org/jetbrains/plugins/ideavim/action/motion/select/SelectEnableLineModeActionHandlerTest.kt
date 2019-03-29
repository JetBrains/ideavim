package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEnableLineModeActionHandlerTest : VimTestCase() {
    fun `test entering select mode`() {
        doTest(parseKeys("gH"),
                """
                A Discovery

                <caret>I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                <selection><caret>I found it in a legendary land</selection>
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test entering select mode at the end of file`() {
        doTest(parseKeys("gH"),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass<caret>.""".trimIndent(),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                <selection>hard by the torrent of a mountain pass<caret>.</selection>""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test entering select mode on empty line`() {
        doTest(parseKeys("gH"),
                """
                A Discovery
                <caret>
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery
                <selection><caret></selection>
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test entering select mode multicaret`() {
        doTest(parseKeys("gH"),
                """
                A Discovery
                <caret>
                <caret>I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was <caret>settled on <caret>some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery
                <selection><caret></selection>
                <selection><caret>I found it in a legendary land</selection>
                all rocks and lavender and tufted grass,
                <selection>where it was <caret>settled on some sodden sand</selection>
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }
}