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

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class MotionPercentOrMatchActionTest : VimTestCase() {
    fun `test percent match simple`() {
        typeTextInFile(parseKeys("%"),
                "foo(b<caret>ar)\n")
        assertOffset(3)
    }

    fun `test percent match multi line`() {
        typeTextInFile(parseKeys("%"),
                """foo(bar,
                     |baz,
                     |<caret>quux)
               """.trimMargin())
        assertOffset(3)
    }

    fun `test percent visual mode match multi line end of line`() {
        typeTextInFile(parseKeys("v$%"),
                """<caret>foo(
                  |bar)""".trimMargin())
        assertOffset(8)
    }

    fun `test percent visual mode match from start multi line end of line`() {
        typeTextInFile(parseKeys("v$%"),
                """<caret>(
                  |bar)""".trimMargin())
        assertOffset(5)
    }

    fun `test percent visual mode find brackets on the end of line`() {
        typeTextInFile(parseKeys("v$%"),
                """foo(<caret>bar)""")
        assertOffset(3)
    }

    fun `test percent twice visual mode find brackets on the end of line`() {
        typeTextInFile(parseKeys("v$%%"),
                """foo(<caret>bar)""")
        assertOffset(7)
    }

    fun `test percent match parens in string`() {
        typeTextInFile(parseKeys("%"),
                """foo(bar, "foo(bar", <caret>baz)
               """)
        assertOffset(3)
    }

    fun `test percent match xml comment start`() {
        configureByXmlText("<caret><!-- foo -->")
        typeText(parseKeys("%"))
        myFixture.checkResult("<!-- foo --<caret>>")
    }

    fun `test percent doesnt match partial xml comment`() {
        configureByXmlText("<!<caret>-- ")
        typeText(parseKeys("%"))
        myFixture.checkResult("<!<caret>-- ")
    }

    fun `test percent match xml comment end`() {
        configureByXmlText("<!-- foo --<caret>>")
        typeText(parseKeys("%"))
        myFixture.checkResult("<caret><!-- foo -->")
    }

    fun `test percent match java comment start`() {
        configureByJavaText("/<caret>* foo */")
        typeText(parseKeys("%"))
        myFixture.checkResult("/* foo *<caret>/")
    }

    fun `test percent doesnt match partial java comment`() {
        configureByJavaText("<caret>/* ")
        typeText(parseKeys("%"))
        myFixture.checkResult("<caret>/* ")
    }

    fun `test percent match java comment end`() {
        configureByJavaText("/* foo <caret>*/")
        typeText(parseKeys("%"))
        myFixture.checkResult("<caret>/* foo */")
    }

    fun `test percent match java doc comment start`() {
        configureByJavaText("/*<caret>* foo */")
        typeText(parseKeys("%"))
        myFixture.checkResult("/** foo *<caret>/")
    }

    fun `test percent match java doc comment end`() {
        configureByJavaText("/** foo *<caret>/")
        typeText(parseKeys("%"))
        myFixture.checkResult("<caret>/** foo */")
    }

    fun `test percent doesnt match after comment start`() {
        configureByJavaText("/*<caret> foo */")
        typeText(parseKeys("%"))
        myFixture.checkResult("/*<caret> foo */")
    }

    fun `test percent doesnt match before comment end`() {
        configureByJavaText("/* foo <caret> */")
        typeText(parseKeys("%"))
        myFixture.checkResult("/* foo <caret> */")
    }

    fun `test motion with quote on the way`() {
        doTest(parseKeys("%"), """
            for (; c!= cj;c = it.next()) <caret>{
             if (dsa) {
               if (c == '\\') {
                 dsadsakkk
               }
             }
            }
        """.trimIndent(),
                """
            for (; c!= cj;c = it.next()) {
             if (dsa) {
               if (c == '\\') {
                 dsadsakkk
               }
             }
            <caret>}
        """.trimIndent())
    }

    fun `test motion outside text`() {
        doTest(parseKeys("%"), """
            (
            ""${'"'}
            ""${'"'} + <caret>title("Display")
            ""${'"'}
            ""${'"'}
            )
        """.trimIndent(),
                """
            (
            ""${'"'}
            ""${'"'} + title("Display"<caret>)
            ""${'"'}
            ""${'"'}
            )
        """.trimIndent())
    }

    fun `test motion in text`() {
        doTest(parseKeys("%"), """ "I found <caret>it in a (legendary) land" """,
                """ "I found it in a (legendary<caret>) land" """)
    }

    fun `test motion in text with quotes`() {
        doTest(parseKeys("%"), """ "I found <caret>it in \"a (legendary) land" """,
                """ "I found it in \"a (legendary<caret>) land" """)
    }

    fun `test motion in text with quotes and double escape`() {
        doTest(parseKeys("%"), """ "I found <caret>it in \\\"a (legendary) land" """,
                """ "I found it in \\\"a (legendary<caret>) land" """)
    }
}