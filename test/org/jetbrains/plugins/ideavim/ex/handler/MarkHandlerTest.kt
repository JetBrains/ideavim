package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class MarkHandlerTest : VimTestCase() {
    fun `test simple mark`() {
        configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it<caret> was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
        typeText(commandToKeys("mark a"))
        VimPlugin.getMark().getMark(myFixture.editor, 'a')?.let {
            assertEquals(2, it.logicalLine)
            assertEquals(0, it.col)
        } ?: TestCase.fail("Mark is null")
    }

    fun `test global mark`() {
        configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it<caret> was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
        typeText(commandToKeys("mark G"))
        VimPlugin.getMark().getMark(myFixture.editor, 'G')?.let {
            assertEquals(2, it.logicalLine)
            assertEquals(0, it.col)
        } ?: TestCase.fail("Mark is null")
    }

    fun `test k mark`() {
        configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it<caret> was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
        typeText(commandToKeys("k a"))
        VimPlugin.getMark().getMark(myFixture.editor, 'a')?.let {
            assertEquals(2, it.logicalLine)
            assertEquals(0, it.col)
        } ?: TestCase.fail("Mark is null")
    }

    fun `test mark in range`() {
        configureByText("""I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it<caret> was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
        typeText(commandToKeys("1,2 mark a"))
        VimPlugin.getMark().getMark(myFixture.editor, 'a')?.let {
            assertEquals(1, it.logicalLine)
            assertEquals(0, it.col)
        } ?: TestCase.fail("Mark is null")
    }
}
