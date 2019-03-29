package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class JoinLinesHandlerTest : VimTestCase() {
    fun `test simple join`() {
        doTest(commandToKeys("j"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found it in a legendary land$c all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test simple join full command`() {
        doTest(commandToKeys("join"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found it in a legendary land$c all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test join with range`() {
        doTest(commandToKeys("4,6j"),
                """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
                """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass, where it was settled on some sodden sand$c hard by the torrent of a mountain pass.
                    """.trimIndent())
    }

    fun `test join multicaret`() {
        configureByText("""
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
        typeText(parseKeys("Vjj"))
        typeText(commandToKeys("join"))
        myFixture.checkResult("""
                A Discovery

                I found it in a legendary land all rocks and lavender and tufted grass,$c where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent())
    }
}