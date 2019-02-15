package org.jetbrains.plugins.ideavim.ex.handler

import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ShiftLeftHandlerTest : VimTestCase() {
    fun `test simple left shift`() {
        val before = """        I found it in a legendary land
                      |        <caret>all rocks and lavender and tufted grass,
                      |        where it was settled on some sodden sand
                      |        hard by the torrent of a mountain pass.
                       """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("<"))

        val after = """        I found it in a legendary land
                      |    <caret>all rocks and lavender and tufted grass,
                      |        where it was settled on some sodden sand
                      |        hard by the torrent of a mountain pass.
                       """.trimMargin()
        myFixture.checkResult(after)
    }

    fun `test double left shift`() {
        val before = """        I found it in a legendary land
                      |        <caret>all rocks and lavender and tufted grass,
                      |        where it was settled on some sodden sand
                      |        hard by the torrent of a mountain pass.
                       """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("<<"))

        val after = """        I found it in a legendary land
                      |<caret>all rocks and lavender and tufted grass,
                      |        where it was settled on some sodden sand
                      |        hard by the torrent of a mountain pass.
                       """.trimMargin()
        myFixture.checkResult(after)
    }

    fun `test left shift no space`() {
        val before = """I found it in a legendary land
                      |<caret>all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                       """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("<"))

        val after = """I found it in a legendary land
                      |<caret>all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                       """.trimMargin()
        myFixture.checkResult(after)
    }

    fun `test range left shift`() {
        val before = """        I found it in a legendary land
                      |        <caret>all rocks and lavender and tufted grass,
                      |        where it was settled on some sodden sand
                      |        hard by the torrent of a mountain pass.
                       """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("3,4<"))

        val after = """        I found it in a legendary land
                      |        all rocks and lavender and tufted grass,
                      |    <caret>where it was settled on some sodden sand
                      |    hard by the torrent of a mountain pass.
                       """.trimMargin()
        myFixture.checkResult(after)
    }

    fun `test multiple carets`() {
        val before = """    I found it in a legendary land
                      |<caret>all rocks and lavender and tufted grass,
                      |    <caret>where it was settled on some sodden sand
                      |    hard by the<caret> torrent of a mountain pass.
                       """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("<"))

        val after = """    I found it in a legendary land
                      |<caret>all rocks and lavender and tufted grass,
                      |<caret>where it was settled on some sodden sand
                      |<caret>hard by the torrent of a mountain pass.
                       """.trimMargin()
        myFixture.checkResult(after)
    }
}