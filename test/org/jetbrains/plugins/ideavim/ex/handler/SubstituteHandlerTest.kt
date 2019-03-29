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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SubstituteHandlerTest : VimTestCase() {
    fun `test one letter`() {
        doTest("s/a/b/",
                """a${c}baba
                 |ab
               """.trimMargin(),
                """bbaba
                 |ab
               """.trimMargin()
        )
    }

    fun `test one letter multi per line`() {
        doTest("s/a/b/g",
                """a${c}baba
                 |ab
               """.trimMargin(),
                """bbbbb
                 |ab
               """.trimMargin()
        )
    }

    fun `test one letter multi per line whole file`() {
        doTest("%s/a/b/g",
                """a${c}baba
                 |ab
               """.trimMargin(),
                """bbbbb
                 |bb
               """.trimMargin()
        )
    }

    // VIM-146
    fun `test eoLto quote`() {
        doTest("s/$/'/g",
                """${c}one
                  |two
               """.trimMargin(),
                """one'
                  |two
               """.trimMargin()
        )
    }

    fun `test soLto quote`() {
        doTest("s/^/'/g",
                """${c}one
                  |two
               """.trimMargin(),
                """'one
                  |two
               """.trimMargin()
        )
    }

    fun `test dot to nul`() {
        doTest("s/\\./\\n/g",
                "${c}one.two.three\n",
                "one\u0000two\u0000three\n")
    }

    // VIM-528
    fun `test groups`() {
        doTest("s/\\(a\\|b\\)/z\\1/g",
                "${c}abcdefg",
                "zazbcdefg")
    }

    fun `test to nl`() {
        doTest("s/\\./\\r/g",
                "${c}one.two.three\n",
                "one\ntwo\nthree\n")
    }

    // VIM-289
    fun `test dot to nlDot`() {
        doTest("s/\\./\\r\\./g",
                "${c}one.two.three\n",
                "one\n.two\n.three\n")
    }

    // VIM-702
    fun `test end of line to nl`() {
        doTest("%s/$/\\r/g",
                "${c}one\ntwo\nthree\n",
                "one\n\ntwo\n\nthree\n\n")
    }

    // VIM-702
    fun `test start of line to nl`() {
        doTest("%s/^/\\r/g",
                "${c}one\ntwo\nthree\n",
                "\none\n\ntwo\n\nthree\n")
    }

    // VIM-864
    fun `test visual substitute doesnt change visual marks`() {
        myFixture.configureByText("a.java", "foo\nbar\nbaz\n")
        typeText(parseKeys("V", "j", ":'<,'>s/foo/fuu/<Enter>", "gv", "~"))
        myFixture.checkResult("FUU\nBAR\nbaz\n")
    }

    fun `test offset range`() {
        doTest(".,+2s/a/b/g",
                "aaa\naa${c}a\naaa\naaa\naaa\n",
                "aaa\nbbb\nbbb\nbbb\naaa\n")
    }

    fun `test multiple carets`() {
        val before = """public class C {
      |  Stri${c}ng a;
      |$c  String b;
      |  Stri${c}ng c;
      |  String d;
      |}
    """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("s/String/Integer"))

        val after = """public class C {
      |  ${c}Integer a;
      |  ${c}Integer b;
      |  ${c}Integer c;
      |  String d;
      |}
    """.trimMargin()
        myFixture.checkResult(after)
    }

    fun `test multiple carets substitute all occurrences`() {
        val before = """public class C {
      |  Stri${c}ng a; String e;
      |$c  String b;
      |  Stri${c}ng c; String f;
      |  String d;
      |}
    """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("s/String/Integer/g"))

        val after = """public class C {
      |  ${c}Integer a; Integer e;
      |  ${c}Integer b;
      |  ${c}Integer c; Integer f;
      |  String d;
      |}
    """.trimMargin()
        myFixture.checkResult(after)
    }

    private fun doTest(command: String, before: String, after: String) {
        myFixture.configureByText("a.java", before)
        typeText(commandToKeys(command))
        myFixture.checkResult(after)
    }
}