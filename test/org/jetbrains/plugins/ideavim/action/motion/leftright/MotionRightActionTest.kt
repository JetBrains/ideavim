package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionRightActionTest : VimTestCase() {
    fun `test simple motion`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found i<caret>t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `test simple motion with repeat`() {
        doTest(parseKeys("3l"), """
            A Discovery

            I found <caret>it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it <caret>in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `test simple motion to the end`() {
        doTest(parseKeys("3l"), """
            A Discovery

            I found it in a legendary lan<caret>d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendary lan<caret>d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `ignore test simple motion non-ascii`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found it in a legendar<caret>𝛁 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar𝛁<caret> land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `ignore test simple motion emoji`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found it in a legendar<caret>🐔 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar🐔<caret> land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }

    fun `test simple motion tab`() {
        doTest(parseKeys("l"), """
            A Discovery

            I found it in a legendar<caret>	 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(), """
            A Discovery

            I found it in a legendar	<caret> land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent())
    }
}