package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ChangeVisualNumberAvalancheDecActionTest : VimTestCase() {
    fun `test dec visual avalanche`() {
        doTest(parseKeys("VGg<C-X>"),
                """
                    <caret>number 2
                    number 3
                    number 4
                    """.trimIndent(),
                """
                    <caret>number 1
                    number 1
                    number 1
                    """.trimIndent())
    }

    fun `test dec visual avalanche multiple times`() {
        doTest(parseKeys("VG2g<C-X>"),
                """
                    <caret>number 3
                    number 5
                    number 7
                    """.trimIndent(),
                """
                    <caret>number 1
                    number 1
                    number 1
                    """.trimIndent())
    }
}