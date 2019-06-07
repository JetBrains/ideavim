/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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

    fun `test execute visual mode`() {
        myFixture.configureByText("a.java", """<caret>123456
            |123456
            |123456
            |123456
            |123456
        """.trimMargin())
        typeText(commandToKeys("normal Vjj"))
        typeText(parseKeys("x"))
        myFixture.checkResult("""
            |<caret>123456
            |123456
        """.trimMargin())
    }

  fun `test execute macros`() {
    myFixture.configureByText("a.java", """<caret>123456
            |123456
            |123456
            |123456
            |123456
        """.trimMargin())
    typeText(parseKeys("qqxq", "jVjjj"))
    typeText(commandToKeys("norm @q"))
    myFixture.checkResult("""23456
            |23456
            |23456
            |<caret>23456
            |123456
        """.trimMargin())
  }

  private fun doTest(command: String, before: String, after: String) {
        myFixture.configureByText("a.java", before)
        typeText(commandToKeys(command))
        myFixture.checkResult(after)
    }
}