package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 *
 * Tests for [com.maddyhome.idea.vim.ex.handler.NormalHandler]
 */
class NormalCommandTest : VimTestCase() {
    fun `test simple execution`() {
        doTest("normal x", "123<caret>456", "123<caret>56")
    }

    fun `test short command`() {
        doTest("norm x", "123<caret>456", "123<caret>56")
    }

    fun `test multiple commands`() {
        doTest("normal xiNewText<Esc>", "123<caret>456", "123NewTex<caret>t56")
    }

    fun `test range single stroke`() {
        doTest(".norm x", "123<caret>456", "<caret>23456")
    }

    fun `test range multiple strokes`() {
        doTest(
                "1,3norm x",
                """
                    123456
                    123456
                    123456<caret>
                    123456
                    123456
                """.trimIndent(),
                """
                    23456
                    23456
                    <caret>23456
                    123456
                    123456
                """.trimIndent()
        )
    }

    fun `test with mapping`() {
        myFixture.configureByText("a.java", """<caret>123456
            |123456
            |123456
        """.trimMargin())
        typeText(commandToKeys("map G dd"))
        typeText(commandToKeys("normal G"))
        myFixture.checkResult("""<caret>123456
            |123456
        """.trimMargin())
    }

    fun `test with disabled mapping`() {
        myFixture.configureByText("a.java", """<caret>123456
            |123456
            |123456
        """.trimMargin())
        typeText(commandToKeys("map G dd"))
        typeText(commandToKeys("normal! G"))
        myFixture.checkResult("""123456
            |123456
            |<caret>123456
        """.trimMargin())
    }

    fun `test from visual mode`() {
        myFixture.configureByText("a.java", """<caret>123456
            |123456
            |123456
            |123456
            |123456
        """.trimMargin())
        typeText(parseKeys("Vjj"))
        typeText(commandToKeys("normal x"))
        myFixture.checkResult("""23456
            |23456
            |<caret>23456
            |123456
            |123456
        """.trimMargin())
    }

    private fun doTest(command: String, before: String, after: String) {
        myFixture.configureByText("a.java", before)
        typeText(commandToKeys(command))
        myFixture.checkResult(after)
    }
}