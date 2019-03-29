package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectToggleVisualModeHandlerTest : VimTestCase() {
    fun `test switch to select mode characterwise`() {
        doTest(parseKeys("ve", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I ${s}found$c$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    }

    fun `test switch to select mode characterwise multicaret`() {
        doTest(parseKeys("ve", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I ${s}found$c$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${s}settled$c$se on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    }

    fun `test switch to visual mode characterwise`() {
        doTest(parseKeys("gh", "<S-Right>".repeat(4), "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I ${s}foun${c}d$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    }

    fun `test switch to visual mode characterwise multicaret`() {
        doTest(parseKeys("gh", "<S-Right>".repeat(4), "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I ${s}foun${c}d$se it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${s}sett${c}l${se}ed on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_CHARACTER)
    }

    fun `test switch to select mode linewise`() {
        doTest(parseKeys("Ve", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                ${s}I found$c it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test switch to select mode linewise multicaret`() {
        doTest(parseKeys("Ve", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary$c land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                ${s}I found$c it in a legendary land$se
                all rocks and lavender and tufted grass,
                ${s}where it was settled$c on some sodden sand$se
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test switch to visual mode linewise`() {
        doTest(parseKeys("gH", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                ${s}I found it in a legendary lan${c}d$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test switch to visual mode linewise multicaret`() {
        doTest(parseKeys("gH", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary$c land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                ${s}I found it in a legendary lan${c}d$se
                all rocks and lavender and tufted grass,
                ${s}where it was settled on some sodden san${c}d$se
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_LINE)
    }

    fun `test switch to select mode blockwise`() {
        doTest(parseKeys("<C-V>ejj", "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I ${s}found$c$se it in a legendary land
                al${s}l roc$c${se}ks and lavender and tufted grass,
                wh${s}ere i$c${se}t was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.SELECT)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }

    fun `test switch to visual mode blockwise`() {
        doTest(parseKeys("g<C-H>", "<S-Right>".repeat(4), "<S-Down>".repeat(2), "<C-G>"),
                """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent(),
                """
                A Discovery

                I ${s}foun${c}d$se it in a legendary land
                al${s}l ro${c}c${se}ks and lavender and tufted grass,
                wh${s}ere ${c}i${se}t was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent())
        assertMode(CommandState.Mode.VISUAL)
        assertSubMode(CommandState.SubMode.VISUAL_BLOCK)
        assertCaretsColour()
    }
}