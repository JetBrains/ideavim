package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ChangeVisualNumberIncActionTest : VimTestCase() {
    fun `test inc visual full number`() {
        doTest(parseKeys("V<C-A>"),
                "<caret>12345",
                "<caret>12346")
    }

    fun `test inc visual multiple numbers`() {
        doTest(parseKeys("v10w<C-A>"),
                "11 <- should not be incremented |<caret>11| should not be incremented -> 12",
                "11 <- should not be incremented |<caret>12| should not be incremented -> 12")
    }

    fun `test inc visual part of number`() {
        doTest(parseKeys("v4l<C-A>"),
                "11111<caret>22222111111",
                "11111<caret>22223111111")
    }

    fun `test inc visual multiple lines`() {
        doTest(parseKeys("V2j<C-A>"),
                """
                    no inc 1
                    no inc 1
                    <caret>inc    5
                    inc   5
                    inc   5
                    no inc 1
                    no inc 1

                    """.trimIndent(),
                """
                    no inc 1
                    no inc 1
                    <caret>inc    6
                    inc   6
                    inc   6
                    no inc 1
                    no inc 1

                    """.trimIndent()
        )
    }

    fun `test inc visual 999 multiple lines`() {
        doTest(parseKeys("V2j<C-A>"),
                """
                    <caret>999
                    999
                    999
                    """.trimIndent(),
                """
                    <caret>1000
                    1000
                    1000
                    """.trimIndent())
    }

    fun `test inc visual multiple numbers on line`() {
        doTest(parseKeys("V<C-A>"),
                "1 should<caret> not be incremented -> 2",
                "<caret>2 should not be incremented -> 2")
    }

    fun `test change number inc visual multiple cursor`() {
        typeTextInFile(parseKeys("Vj<C-A>"),
                """
                    <caret>1
                    2
                    3
                    <caret>4
                    5
                    """.trimIndent())
        myFixture.checkResult(
                """
                    <caret>2
                    3
                    3
                    <caret>5
                    6
                    """.trimIndent())
    }
}