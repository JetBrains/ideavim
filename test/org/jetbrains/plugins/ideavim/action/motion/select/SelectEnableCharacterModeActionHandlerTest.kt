package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEnableCharacterModeActionHandlerTest : VimTestCase() {
    fun `test entering select mode`() {
        doTest(parseKeys("gh"),
                """
                A Discovery

                <caret>I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                <selection>I<caret></selection> found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertMode(CommandState.Mode.SELECT)
    }

    fun `test entering select mode at the end of file`() {
        doTest(parseKeys("gh"),
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
                hard by the torrent of a mountain pass<selection>.<caret></selection>""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
    }

    fun `test entering select mode on empty line`() {
        doTest(parseKeys("gh"),
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
    }

    fun `test entering select mode multicaret`() {
        doTest(parseKeys("gh"),
                """
                A Discovery
                <caret>
                <caret>I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was <caret>settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery
                <selection><caret></selection>
                <selection>I<caret></selection> found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was <selection>s<caret></selection>ettled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
    }
}