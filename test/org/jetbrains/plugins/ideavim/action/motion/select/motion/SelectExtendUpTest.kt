package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectExtendUpTest : VimTestCase() {
    fun `test char mode simple motion`() {
        doTest(parseKeys("gh", "<S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I$s$c found it in a legendary land
                ${se}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test char mode to empty line`() {
        doTest(parseKeys("gh", "<S-Up>"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery
                $s$c
                ${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test char mode from empty line`() {
        doTest(parseKeys("gh", "<S-Up>"),
                """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s${c}A Discovery
                $se
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test char mode on file start`() {
        doTest(parseKeys("gh", "<S-Up>"),
                """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A ${s}D$c${se}iscovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test char mode multicaret`() {
        doTest(parseKeys("gh", "<S-Up>"),
                """
                A ${c}Discovery

                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A ${s}D$c${se}iscovery
                $s$c
                I found ${se}it in a legendary land
                all rocks and $s${c}lavender and tufted grass,
                where it was ${se}settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode simple motion`() {
        doTest(parseKeys("gH", "<S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                $s${c}I found it in a legendary land
                all rocks and lavender and tufted grass,${se}
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode to empty line`() {
        doTest(parseKeys("gH", "<S-Up>"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery
                $s$c
                I found it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode from empty line`() {
        doTest(parseKeys("gH", "<S-Up>"),
                """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s${c}A Discovery
                $se
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode to line start`() {
        doTest(parseKeys("gH", "<S-Up>"),
                """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                ${s}A ${c}Discovery$se

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test line mode multicaret`() {
        doTest(parseKeys("gH", "<S-Up>"),
                """
                A ${c}Discovery

                I found it in a legendary land
                all rocks ${c}and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                ${s}A ${c}Discovery$se

                ${s}I found it$c in a legendary land
                all rocks and lavender and tufted grass,$se
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test block mode simple motion`() {
        doTest(parseKeys("g<C-H>", "<S-Up>"),
                """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                ${s}I$c$se found it in a legendary land
                ${s}a$c${se}ll rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test block mode to empty line`() {
        doTest(parseKeys("g<C-H>", "<S-Up>"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery
                $s$c$se
                ${s}$c${se}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test block mode from empty line`() {
        doTest(parseKeys("g<C-H>", "<S-Up>"),
                """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                $s$c${se}A Discovery
                $s$c$se
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test block mode to line start`() {
        doTest(parseKeys("g<C-H>", "<S-Up>"),
                """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A ${s}D$c${se}iscovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }
}