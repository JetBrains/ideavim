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

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ChangeVisualNumberDecActionTest : VimTestCase() {
    fun `test dec visual full number`() {
        doTest(parseKeys("V<C-X>"),
                "<caret>12345",
                "<caret>12344")
    }

    fun `test dec visual multiple numbers`() {
        doTest(parseKeys("v10w<C-X>"),
                "11 <- should not be decremented |<caret>11| should not be decremented -> 12",
                "11 <- should not be decremented |<caret>10| should not be decremented -> 12")
    }

    fun `test dec visual part of number`() {
        doTest(parseKeys("v4l<C-X>"),
                "11111<caret>33333111111",
                "11111<caret>33332111111")
    }

    fun `test dec visual multiple lines`() {
        doTest(parseKeys("V2j<C-X>"),
                """
                    no dec 1
                    no dec 1
                    <caret>dec    5
                    dec   5
                    dec   5
                    no dec 1
                    no dec 1

                    """.trimIndent(),
                """
                    no dec 1
                    no dec 1
                    <caret>dec    4
                    dec   4
                    dec   4
                    no dec 1
                    no dec 1

                    """.trimIndent()
        )
    }

    fun `test dec visual 1000 multiple lines`() {
        doTest(parseKeys("V2j<C-X>"),
                """
                    <caret>1000
                    1000
                    1000
                    """.trimIndent(),
                """
                    <caret>999
                    999
                    999
                    """.trimIndent())
    }

    fun `test dec visual multiple numbers on line`() {
        doTest(parseKeys("V<C-X>"),
                "1 should<caret> not be decremented -> 2",
                "<caret>0 should not be decremented -> 2")
    }

    fun `test change number dec visual action`() {
        typeTextInFile(parseKeys("Vj<C-X>"),
                """
                    <caret>1
                    2
                    3
                    <caret>4
                    5
                    """.trimIndent())
        myFixture.checkResult("""
                <caret>0
                1
                3
                <caret>3
                4
                """.trimIndent())
    }
}