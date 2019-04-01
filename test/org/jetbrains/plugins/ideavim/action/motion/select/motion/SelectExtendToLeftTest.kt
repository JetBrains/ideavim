package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectExtendToLeftTest : VimTestCase() {
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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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

                ${s}I ${c}found it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

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

                $s${c}I found it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

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
                $s${c}A Discovery$se

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

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
                $s${c}A Discovery$se

                ${s}I found it ${c}in a legendary land$se
                ${s}all rocks$c and lavender and tufted grass,$se
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
    }

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
                    """.trimIndent())
        assertCaretsColour()
    }

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

                I fo${s}u${c}n${se}d it in a legendary land
                all ${s}r${c}o${se}cks and lavender and tufted grass,
                wher$s${c}e ${se}it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        assertCaretsColour()
    }
}