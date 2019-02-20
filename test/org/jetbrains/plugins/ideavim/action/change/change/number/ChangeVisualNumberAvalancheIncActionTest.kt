package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ChangeVisualNumberAvalancheIncActionTest : VimTestCase() {
    fun `test inc visual avalanche`() {
        doTest(parseKeys("VGg<C-A>"),
                """
                    <caret>number 1
                    number 1
                    number 1
                    """.trimIndent(),
                """
                    <caret>number 2
                    number 3
                    number 4
                    """.trimIndent())
    }

    fun `test inc visual avalanche multiple times`() {
        doTest(parseKeys("VG2g<C-A>"),
                """
                    <caret>number 1
                    number 1
                    number 1
                    """.trimIndent(),
                """
                    <caret>number 3
                    number 5
                    number 7
                    """.trimIndent())
    }
}